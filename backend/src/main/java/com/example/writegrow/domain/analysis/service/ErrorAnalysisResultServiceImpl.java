package com.example.writegrow.domain.analysis.service;

import com.example.writegrow.domain.activity.entity.ActivityEventType;
import com.example.writegrow.domain.activity.service.ActivityEventService;
import com.example.writegrow.domain.analysis.entity.ErrorAnalysis;
import com.example.writegrow.domain.analysis.entity.ErrorCandidate;
import com.example.writegrow.domain.analysis.entity.ErrorProfile;
import com.example.writegrow.domain.analysis.entity.ErrorType;
import com.example.writegrow.domain.analysis.repository.ErrorAnalysisRepository;
import com.example.writegrow.domain.analysis.repository.ErrorProfileRepository;
import com.example.writegrow.global.config.properties.ErrorAnalysisProperties;
import com.example.writegrow.infra.ai.dto.AiErrorAnalysisResponse;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorAnalysisResultServiceImpl implements ErrorAnalysisResultService {

    private final ErrorAnalysisRepository errorAnalysisRepository;
    private final ErrorProfileRepository errorProfileRepository;
    private final ActivityEventService activityEventService;
    private final ErrorAnalysisProperties errorAnalysisProperties;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProcessing(Long writingId, Long profileId, String analyzedText) {
        ErrorAnalysis analysis = findOrCreate(writingId, profileId);
        analysis.markProcessing(analyzedText);
        errorAnalysisRepository.save(analysis);

        activityEventService.record(ActivityEventType.ERROR_ANALYSIS_REQUESTED, profileId, writingId);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeAnalysis(Long writingId, Long profileId, String provider,
                                 AiErrorAnalysisResponse response) {
        ErrorAnalysis analysis = findOrCreate(writingId, profileId);
        applyCandidates(analysis, response.errors());
        analysis.markSucceeded(provider);
        errorAnalysisRepository.save(analysis);

        // 확정 오류만 프로필에 쌓는다. 낮은 확신도 후보를 집계하면 아동이 실제로 틀리지 않은 것이
        // 반복 오류로 잡히고, 그 값이 미션과 리포트로 흘러간다. (REQ-03 비즈니스 규칙 4)
        List<ErrorCandidate> confirmed = analysis.confirmedCandidates();
        updateErrorProfile(profileId, confirmed);

        activityEventService.record(ActivityEventType.ERROR_ANALYSIS_COMPLETED, profileId, writingId, Map.of(
                "confirmedCount", confirmed.size(),
                "reviewCount", analysis.reviewCandidates().size()));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failAnalysis(Long writingId, Long profileId, String reason) {
        ErrorAnalysis analysis = findOrCreate(writingId, profileId);
        analysis.markFailed(reason);
        errorAnalysisRepository.save(analysis);

        // 실패해도 교정 안내를 만들지 않는다. 상태만 남겨 이후 재처리할 수 있게 한다.
        // (REQ-03 예외 규칙)
        activityEventService.record(ActivityEventType.ERROR_ANALYSIS_FAILED, profileId, writingId,
                Map.of("reason", reason == null ? "unknown" : reason));
    }

    private ErrorAnalysis findOrCreate(Long writingId, Long profileId) {
        return errorAnalysisRepository.findWithCandidatesByWritingId(writingId)
                .orElseGet(() -> ErrorAnalysis.create(writingId, profileId));
    }

    private void applyCandidates(ErrorAnalysis analysis, List<AiErrorAnalysisResponse.ErrorItem> errors) {
        if (errors == null) {
            return;
        }
        double threshold = errorAnalysisProperties.confidenceThreshold();
        int seq = 0;
        for (AiErrorAnalysisResponse.ErrorItem item : errors) {
            Optional<ErrorType> errorType = ErrorType.from(item.type());
            if (errorType.isEmpty()) {
                // 모르는 유형 하나 때문에 분석 전체를 실패로 만들지 않는다.
                log.warn("알 수 없는 오류 유형을 건너뜁니다: writingId={}, type={}",
                        analysis.getWritingId(), item.type());
                continue;
            }
            double confidence = item.confidence() == null ? 0.0 : item.confidence();
            analysis.addCandidate(
                    seq++,
                    errorType.get(),
                    item.startIndex(),
                    item.endIndex(),
                    item.original(),
                    item.suggestion(),
                    confidence,
                    item.reason(),
                    confidence < threshold);
        }
    }

    private void updateErrorProfile(Long profileId, List<ErrorCandidate> confirmed) {
        if (confirmed.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        Map<ErrorType, Long> countsByType = confirmed.stream()
                .collect(Collectors.groupingBy(ErrorCandidate::getErrorType, Collectors.counting()));

        countsByType.forEach((errorType, count) -> {
            ErrorProfile profile = errorProfileRepository
                    .findByProfileIdAndErrorType(profileId, errorType)
                    .orElseGet(() -> ErrorProfile.create(profileId, errorType));
            profile.recordOccurrences(count.intValue(), today);
            errorProfileRepository.save(profile);
        });
    }
}
