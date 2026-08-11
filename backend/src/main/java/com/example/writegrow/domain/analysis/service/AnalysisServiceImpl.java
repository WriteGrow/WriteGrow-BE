package com.example.writegrow.domain.analysis.service;

import com.example.writegrow.domain.analysis.dto.response.AnalysisResponse;
import com.example.writegrow.domain.analysis.dto.response.ProcessMetricResponse;
import com.example.writegrow.domain.analysis.entity.OcrResult;
import com.example.writegrow.domain.analysis.exception.AnalysisErrorCode;
import com.example.writegrow.domain.analysis.exception.AnalysisException;
import com.example.writegrow.domain.analysis.repository.OcrResultRepository;
import com.example.writegrow.domain.analysis.repository.WritingProcessMetricRepository;
import com.example.writegrow.domain.handwriting.dto.response.HandwritingAnalysisSource;
import com.example.writegrow.domain.handwriting.exception.HandwritingErrorCode;
import com.example.writegrow.domain.handwriting.exception.HandwritingException;
import com.example.writegrow.domain.handwriting.service.HandwritingQueryService;
import com.example.writegrow.domain.handwriting.service.HandwritingService;
import com.example.writegrow.domain.writing.entity.Writing;
import com.example.writegrow.domain.writing.event.HandwritingSubmittedEvent;
import com.example.writegrow.domain.writing.exception.WritingErrorCode;
import com.example.writegrow.domain.writing.exception.WritingException;
import com.example.writegrow.domain.writing.repository.WritingRepository;
import com.example.writegrow.infra.ai.AiAnalysisClient;
import com.example.writegrow.infra.ai.dto.AiAnalysisRequest;
import com.example.writegrow.infra.ai.dto.AiAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisServiceImpl implements AnalysisService {

    private final AnalysisResultService analysisResultService;
    private final HandwritingService handwritingService;
    private final HandwritingQueryService handwritingQueryService;
    private final OcrResultRepository ocrResultRepository;
    private final WritingProcessMetricRepository writingProcessMetricRepository;
    private final WritingRepository writingRepository;
    private final AiAnalysisClient aiAnalysisClient;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 오래 걸리는 AI 호출을 트랜잭션 밖에 두고, 상태 변경만 {@link AnalysisResultService} 의 짧은 트랜잭션으로 처리한다.
     */
    @Override
    public void runAnalysis(Long writingId, Long profileId) {
        analysisResultService.markProcessing(writingId, profileId);

        try {
            String topic = writingRepository.findById(writingId)
                    .map(Writing::getTopic)
                    .orElse(null);

            HandwritingAnalysisSource source = handwritingService.finalizeForAnalysis(writingId);

            AiAnalysisResponse response = aiAnalysisClient.analyze(new AiAnalysisRequest(
                    writingId,
                    source.imageUrl(),
                    source.strokeDataUrl(),
                    new AiAnalysisRequest.Canvas(source.canvasWidth(), source.canvasHeight()),
                    topic));

            analysisResultService.completeAnalysis(writingId, profileId, aiAnalysisClient.provider(), response);
            log.info("손글씨 분석 완료: writingId={}, strokeCount={}", writingId, source.strokeCount());
        } catch (Exception exception) {
            log.error("손글씨 분석 실패: writingId={}", writingId, exception);
            analysisResultService.failAnalysis(writingId, profileId, exception.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public AnalysisResponse getAnalysis(Long profileId, Long writingId) {
        findOwnedHandwriting(profileId, writingId);

        OcrResult ocrResult = ocrResultRepository.findWithSegmentsByWritingId(writingId)
                .orElseThrow(() -> new AnalysisException(AnalysisErrorCode.ANALYSIS_NOT_FOUND));

        ProcessMetricResponse processMetric = writingProcessMetricRepository.findByWritingId(writingId)
                .map(ProcessMetricResponse::from)
                .orElse(null);

        return AnalysisResponse.of(
                ocrResult,
                processMetric,
                handwritingQueryService.findByWritingId(writingId).orElse(null));
    }

    @Override
    @Transactional
    public void retryAnalysis(Long profileId, Long writingId) {
        Writing writing = findOwnedHandwriting(profileId, writingId);

        OcrResult ocrResult = ocrResultRepository.findByWritingId(writingId)
                .orElseThrow(() -> new AnalysisException(AnalysisErrorCode.ANALYSIS_NOT_FOUND));
        if (!ocrResult.isFailed()) {
            throw new AnalysisException(AnalysisErrorCode.NOT_FAILED_STATE);
        }

        writing.markAnalysisRetried();
        eventPublisher.publishEvent(new HandwritingSubmittedEvent(writingId, profileId));
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
}
