package com.example.writegrow.domain.handwriting.service;

import com.example.writegrow.domain.handwriting.dto.response.HandwritingSummaryResponse;
import com.example.writegrow.domain.handwriting.entity.HandwritingAsset;
import com.example.writegrow.domain.handwriting.repository.HandwritingAssetRepository;
import com.example.writegrow.infra.s3.StorageClient;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class HandwritingQueryServiceImpl implements HandwritingQueryService {

    private final HandwritingAssetRepository handwritingAssetRepository;
    private final StorageClient storageClient;

    @Override
    @Transactional(readOnly = true)
    public Optional<HandwritingSummaryResponse> findByWritingId(Long writingId) {
        return handwritingAssetRepository.findByWritingId(writingId).map(this::toSummary);
    }

    private HandwritingSummaryResponse toSummary(HandwritingAsset asset) {
        return new HandwritingSummaryResponse(
                asset.hasImage() ? storageClient.presignedGetUrl(asset.getImageObjectKey()) : null,
                asset.hasStrokeData() ? storageClient.presignedGetUrl(asset.getStrokeObjectKey()) : null,
                asset.getStrokeCount(),
                asset.getTotalDurationMs(),
                asset.getCanvasWidth(),
                asset.getCanvasHeight());
    }
}
