package com.example.writegrow.domain.handwriting.service;

import com.example.writegrow.domain.handwriting.dto.request.StrokeBatchAppendRequest;
import com.example.writegrow.domain.handwriting.dto.response.HandwritingAnalysisSource;
import com.example.writegrow.domain.handwriting.dto.response.HandwritingImageUploadResponse;
import com.example.writegrow.domain.handwriting.dto.response.MergedStrokeDocument;
import com.example.writegrow.domain.handwriting.dto.response.StrokeBatchAppendResponse;
import com.example.writegrow.domain.handwriting.entity.HandwritingAsset;
import com.example.writegrow.domain.handwriting.entity.StrokeBatch;
import com.example.writegrow.domain.handwriting.entity.StrokePayload;
import com.example.writegrow.domain.handwriting.exception.HandwritingErrorCode;
import com.example.writegrow.domain.handwriting.exception.HandwritingException;
import com.example.writegrow.domain.handwriting.repository.HandwritingAssetRepository;
import com.example.writegrow.domain.handwriting.repository.StrokeBatchRepository;
import com.example.writegrow.domain.handwriting.service.StrokeMerger.MergedStrokes;
import com.example.writegrow.domain.writing.entity.Writing;
import com.example.writegrow.domain.writing.exception.WritingErrorCode;
import com.example.writegrow.domain.writing.exception.WritingException;
import com.example.writegrow.domain.writing.repository.WritingRepository;
import com.example.writegrow.infra.s3.StorageClient;
import com.example.writegrow.infra.s3.StorageKeyFactory;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class HandwritingServiceImpl implements HandwritingService {

    private static final Map<String, String> ALLOWED_IMAGE_TYPES = Map.of(
            MediaType.IMAGE_PNG_VALUE, "png",
            MediaType.IMAGE_JPEG_VALUE, "jpg");

    private static final String STROKE_CONTENT_TYPE = MediaType.APPLICATION_JSON_VALUE;

    private final StrokeBatchRepository strokeBatchRepository;
    private final HandwritingAssetRepository handwritingAssetRepository;
    private final WritingRepository writingRepository;
    private final StorageClient storageClient;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public StrokeBatchAppendResponse appendStrokeBatch(Long profileId, Long writingId,
                                                       StrokeBatchAppendRequest request) {
        findOwnedHandwriting(profileId, writingId);

        if (request.strokes() == null || request.strokes().isEmpty()) {
            throw new HandwritingException(HandwritingErrorCode.EMPTY_STROKE_BATCH);
        }

        // 같은 배치를 다시 받아도 중복 저장하지 않는다. 네트워크 재시도에 안전하다.
        if (strokeBatchRepository.existsByWritingIdAndBatchSeq(writingId, request.batchSeq())) {
            log.debug("이미 수신한 획 배치입니다: writingId={}, batchSeq={}", writingId, request.batchSeq());
            return new StrokeBatchAppendResponse(writingId, request.batchSeq(), request.strokes().size(),
                    strokeBatchRepository.countByWritingId(writingId), true);
        }

        StrokeBatch batch = strokeBatchRepository.save(
                StrokeBatch.of(writingId, request.batchSeq(), StrokePayload.of(request.strokes())));

        return new StrokeBatchAppendResponse(writingId, batch.getBatchSeq(), batch.getStrokeCount(),
                strokeBatchRepository.countByWritingId(writingId), false);
    }

    @Override
    @Transactional
    public HandwritingImageUploadResponse uploadImage(Long profileId, Long writingId, MultipartFile file,
                                                      Integer canvasWidth, Integer canvasHeight) {
        findOwnedHandwriting(profileId, writingId);

        if (file == null || file.isEmpty()) {
            throw new HandwritingException(HandwritingErrorCode.EMPTY_IMAGE);
        }
        String extension = ALLOWED_IMAGE_TYPES.get(file.getContentType());
        if (extension == null) {
            throw new HandwritingException(HandwritingErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }

        byte[] content = readBytes(file);
        String key = storageClient.upload(
                StorageKeyFactory.handwritingImageKey(writingId, extension), content, file.getContentType());

        HandwritingAsset asset = handwritingAssetRepository.findByWritingId(writingId)
                .orElseGet(() -> HandwritingAsset.create(writingId));
        asset.updateImage(key, canvasWidth, canvasHeight);
        handwritingAssetRepository.save(asset);

        return new HandwritingImageUploadResponse(writingId, storageClient.presignedGetUrl(key));
    }

    @Override
    @Transactional
    public HandwritingAnalysisSource finalizeForAnalysis(Long writingId) {
        HandwritingAsset asset = handwritingAssetRepository.findByWritingId(writingId)
                .orElseThrow(() -> new HandwritingException(HandwritingErrorCode.IMAGE_REQUIRED));
        if (!asset.hasImage()) {
            throw new HandwritingException(HandwritingErrorCode.IMAGE_REQUIRED);
        }

        List<StrokeBatch> batches = strokeBatchRepository.findAllByWritingIdOrderByBatchSeqAsc(writingId);
        if (batches.isEmpty()) {
            throw new HandwritingException(HandwritingErrorCode.STROKE_DATA_REQUIRED);
        }

        MergedStrokes merged = StrokeMerger.merge(batches);
        MergedStrokeDocument document = new MergedStrokeDocument(
                writingId, asset.getCanvasWidth(), asset.getCanvasHeight(),
                merged.strokeCount(), merged.totalDurationMs(), merged.strokes());

        String strokeKey = storageClient.upload(
                StorageKeyFactory.strokeDataKey(writingId), serialize(document), STROKE_CONTENT_TYPE);

        asset.updateStrokeData(strokeKey, merged.strokeCount(), merged.totalDurationMs());
        handwritingAssetRepository.save(asset);

        return new HandwritingAnalysisSource(
                writingId,
                storageClient.presignedGetUrl(asset.getImageObjectKey()),
                storageClient.presignedGetUrl(strokeKey),
                asset.getCanvasWidth(),
                asset.getCanvasHeight(),
                merged.strokeCount(),
                merged.totalDurationMs());
    }

    private Writing findOwnedHandwriting(Long profileId, Long writingId) {
        Writing writing = writingRepository.findById(writingId)
                .orElseThrow(() -> new WritingException(WritingErrorCode.WRITING_NOT_FOUND));
        writing.validateOwner(profileId);
        if (!writing.isHandwriting()) {
            throw new HandwritingException(HandwritingErrorCode.NOT_HANDWRITING);
        }
        return writing;
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException exception) {
            throw new HandwritingException(HandwritingErrorCode.IMAGE_READ_FAILED, exception);
        }
    }

    private byte[] serialize(MergedStrokeDocument document) {
        try {
            return objectMapper.writeValueAsBytes(document);
        } catch (JacksonException exception) {
            throw new HandwritingException(HandwritingErrorCode.STROKE_SERIALIZATION_FAILED, exception);
        }
    }
}
