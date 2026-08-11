package com.example.writegrow.domain.analysis.service;

import com.example.writegrow.domain.activity.entity.ActivityEventType;
import com.example.writegrow.domain.activity.service.ActivityEventService;
import com.example.writegrow.domain.analysis.entity.HesitationPoint;
import com.example.writegrow.domain.analysis.entity.OcrResult;
import com.example.writegrow.domain.analysis.entity.WritingProcessMetric;
import com.example.writegrow.domain.analysis.repository.OcrResultRepository;
import com.example.writegrow.domain.analysis.repository.WritingProcessMetricRepository;
import com.example.writegrow.domain.writing.entity.Writing;
import com.example.writegrow.domain.writing.exception.WritingErrorCode;
import com.example.writegrow.domain.writing.exception.WritingException;
import com.example.writegrow.domain.writing.repository.WritingRepository;
import com.example.writegrow.global.config.properties.OcrProperties;
import com.example.writegrow.infra.ai.dto.AiAnalysisResponse;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnalysisResultServiceImpl implements AnalysisResultService {

    private final OcrResultRepository ocrResultRepository;
    private final WritingProcessMetricRepository writingProcessMetricRepository;
    private final WritingRepository writingRepository;
    private final ActivityEventService activityEventService;
    private final OcrProperties ocrProperties;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessing(Long writingId, Long profileId) {
        OcrResult ocrResult = findOrCreateResult(writingId);
        ocrResult.markProcessing();
        ocrResultRepository.save(ocrResult);

        activityEventService.record(ActivityEventType.OCR_REQUESTED, profileId, writingId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeAnalysis(Long writingId, Long profileId, String provider, AiAnalysisResponse response) {
        OcrResult ocrResult = findOrCreateResult(writingId);
        ocrResult.markSucceeded(response.fullText(), response.overallConfidence(), provider);
        applySegments(ocrResult, response.segments());
        ocrResultRepository.save(ocrResult);

        Writing writing = writingRepository.findWithRevisionsById(writingId)
                .orElseThrow(() -> new WritingException(WritingErrorCode.WRITING_NOT_FOUND));
        writing.applyOcrText(response.fullText());

        saveProcessMetric(writingId, response.processMetric());

        long lowConfidenceCount = ocrResult.getSegments().stream()
                .filter(segment -> segment.isLowConfidence())
                .count();
        activityEventService.record(ActivityEventType.OCR_COMPLETED, profileId, writingId, Map.of(
                "segmentCount", ocrResult.getSegments().size(),
                "lowConfidenceCount", lowConfidenceCount));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failAnalysis(Long writingId, Long profileId, String reason) {
        OcrResult ocrResult = findOrCreateResult(writingId);
        ocrResult.markFailed(reason);
        ocrResultRepository.save(ocrResult);

        // 손글씨 원본과 획 데이터는 그대로 보존한다. (기능명세서 FEAT-02-01 예외 규칙)
        writingRepository.findById(writingId).ifPresent(Writing::markAnalysisFailed);

        activityEventService.record(ActivityEventType.OCR_FAILED, profileId, writingId,
                Map.of("reason", reason == null ? "unknown" : reason));
    }

    private OcrResult findOrCreateResult(Long writingId) {
        return ocrResultRepository.findWithSegmentsByWritingId(writingId)
                .orElseGet(() -> OcrResult.create(writingId));
    }

    private void applySegments(OcrResult ocrResult, List<AiAnalysisResponse.Segment> segments) {
        if (segments == null) {
            return;
        }
        double threshold = ocrProperties.confidenceThreshold();
        int seq = 0;
        for (AiAnalysisResponse.Segment segment : segments) {
            double confidence = segment.confidence() == null ? 0.0 : segment.confidence();
            ocrResult.addSegment(
                    seq++,
                    segment.text(),
                    confidence,
                    segment.startIndex(),
                    segment.endIndex(),
                    confidence < threshold);
        }
    }

    private void saveProcessMetric(Long writingId, AiAnalysisResponse.ProcessMetric source) {
        if (source == null) {
            return;
        }
        WritingProcessMetric metric = writingProcessMetricRepository.findByWritingId(writingId)
                .orElseGet(() -> WritingProcessMetric.create(writingId));

        List<HesitationPoint> hesitationPoints = source.hesitationPoints() == null
                ? List.of()
                : source.hesitationPoints().stream()
                .map(point -> new HesitationPoint(
                        point.charIndex(), point.character(), point.jamo(),
                        point.durationMs(), point.retryCount()))
                .toList();

        metric.update(source.totalDurationMs(), source.pauseCount(), source.longestPauseMs(),
                source.avgStrokeDurationMs(), hesitationPoints);
        writingProcessMetricRepository.save(metric);
    }
}
