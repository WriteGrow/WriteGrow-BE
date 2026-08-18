package com.example.writegrow.domain.analysis.service;

import com.example.writegrow.domain.account.service.ProfileService;
import com.example.writegrow.domain.activity.entity.ActivityEventType;
import com.example.writegrow.domain.activity.service.ActivityEventService;
import com.example.writegrow.domain.analysis.dto.response.ErrorProfileResponse;
import com.example.writegrow.domain.analysis.dto.response.ErrorReviewResponse;
import com.example.writegrow.domain.analysis.dto.response.WritingErrorsResponse;
import com.example.writegrow.domain.analysis.entity.ErrorAnalysis;
import com.example.writegrow.domain.analysis.exception.AnalysisErrorCode;
import com.example.writegrow.domain.analysis.exception.AnalysisException;
import com.example.writegrow.domain.analysis.repository.ErrorAnalysisRepository;
import com.example.writegrow.domain.analysis.repository.ErrorProfileRepository;
import com.example.writegrow.domain.writing.entity.Writing;
import com.example.writegrow.domain.writing.exception.WritingErrorCode;
import com.example.writegrow.domain.writing.exception.WritingException;
import com.example.writegrow.domain.writing.repository.WritingRepository;
import com.example.writegrow.infra.ai.AiAnalysisClient;
import com.example.writegrow.infra.ai.dto.AiErrorAnalysisRequest;
import com.example.writegrow.infra.ai.dto.AiErrorAnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class ErrorAnalysisServiceImpl implements ErrorAnalysisService {

    private final ErrorAnalysisResultService errorAnalysisResultService;
    private final ErrorAnalysisRepository errorAnalysisRepository;
    private final ErrorProfileRepository errorProfileRepository;
    private final WritingRepository writingRepository;
    private final ProfileService profileService;
    private final ActivityEventService activityEventService;
    private final AiAnalysisClient aiAnalysisClient;

    /**
     * AI 호출을 트랜잭션 밖에 두고, 상태 변경만 {@link ErrorAnalysisResultService} 의 짧은 트랜잭션으로 처리한다.
     */
    @Override
    public void runAnalysis(Long writingId, Long profileId) {
        Writing writing = writingRepository.findById(writingId).orElse(null);
        if (writing == null) {
            log.warn("오류 분석 대상 글이 없습니다: writingId={}", writingId);
            return;
        }

        String text = writing.getFinalText();
        if (text == null || text.isBlank()) {
            // 확정 텍스트가 없으면 분석할 대상이 없다. 선행조건 미충족이므로 실패로 남기지 않는다.
            log.warn("확정 텍스트가 없어 오류 분석을 건너뜁니다: writingId={}", writingId);
            return;
        }

        errorAnalysisResultService.markProcessing(writingId, profileId, text);

        try {
            AiErrorAnalysisResponse response = aiAnalysisClient.analyzeText(
                    new AiErrorAnalysisRequest(writingId, text, writing.getTopic()));

            errorAnalysisResultService.completeAnalysis(
                    writingId, profileId, aiAnalysisClient.provider(), response);
            log.info("오류 분석 완료: writingId={}", writingId);
        } catch (Exception exception) {
            log.error("오류 분석 실패: writingId={}", writingId, exception);
            errorAnalysisResultService.failAnalysis(writingId, profileId, exception.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public WritingErrorsResponse getConfirmedErrors(Long profileId, Long writingId) {
        Writing writing = writingRepository.findById(writingId)
                .orElseThrow(() -> new WritingException(WritingErrorCode.WRITING_NOT_FOUND));
        writing.validateOwner(profileId);

        return WritingErrorsResponse.from(findAnalysis(writingId));
    }

    @Override
    @Transactional
    public ErrorReviewResponse getReviewCandidates(Long viewerProfileId, Long writingId) {
        Writing writing = writingRepository.findById(writingId)
                .orElseThrow(() -> new WritingException(WritingErrorCode.WRITING_NOT_FOUND));
        // 글을 쓴 아동을 조회자가 볼 수 있는지 확인한다. 본인 글이거나 같은 계정의 보호자여야 한다.
        profileService.getViewableChild(viewerProfileId, writing.getProfileId());

        ErrorReviewResponse response = ErrorReviewResponse.from(findAnalysis(writingId));

        // 명세가 검토 대상 열람 이벤트 기록을 요구한다. (FEAT-03-02 결과)
        activityEventService.record(ActivityEventType.ERROR_REVIEW_VIEWED, viewerProfileId, writingId);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public ErrorProfileResponse getErrorProfile(Long viewerProfileId, Long childProfileId) {
        profileService.getViewableChild(viewerProfileId, childProfileId);

        return ErrorProfileResponse.of(
                childProfileId,
                errorProfileRepository.findAllByProfileIdOrderByOccurrenceCountDesc(childProfileId));
    }

    private ErrorAnalysis findAnalysis(Long writingId) {
        return errorAnalysisRepository.findWithCandidatesByWritingId(writingId)
                .orElseThrow(() -> new AnalysisException(AnalysisErrorCode.ANALYSIS_NOT_FOUND));
    }
}
