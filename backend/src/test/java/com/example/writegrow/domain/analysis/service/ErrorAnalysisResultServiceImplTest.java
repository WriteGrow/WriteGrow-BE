package com.example.writegrow.domain.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.writegrow.domain.activity.entity.ActivityEventType;
import com.example.writegrow.domain.activity.service.ActivityEventService;
import com.example.writegrow.domain.analysis.entity.ErrorAnalysis;
import com.example.writegrow.domain.analysis.entity.ErrorProfile;
import com.example.writegrow.domain.analysis.entity.ErrorType;
import com.example.writegrow.domain.analysis.repository.ErrorAnalysisRepository;
import com.example.writegrow.domain.analysis.repository.ErrorProfileRepository;
import com.example.writegrow.global.config.properties.ErrorAnalysisProperties;
import com.example.writegrow.infra.ai.dto.AiErrorAnalysisResponse;
import com.example.writegrow.infra.ai.dto.AiErrorAnalysisResponse.ErrorItem;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ErrorAnalysisResultServiceImpl 단위 테스트")
class ErrorAnalysisResultServiceImplTest {

    private static final Long WRITING_ID = 100L;
    private static final Long PROFILE_ID = 1L;
    private static final double THRESHOLD = 0.75;

    @Mock
    private ErrorAnalysisRepository errorAnalysisRepository;

    @Mock
    private ErrorProfileRepository errorProfileRepository;

    @Mock
    private ActivityEventService activityEventService;

    private ErrorAnalysisResultServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ErrorAnalysisResultServiceImpl(
                errorAnalysisRepository, errorProfileRepository, activityEventService,
                new ErrorAnalysisProperties(THRESHOLD));
    }

    private void givenNewAnalysis() {
        given(errorAnalysisRepository.findWithCandidatesByWritingId(WRITING_ID))
                .willReturn(Optional.empty());
    }

    private ErrorAnalysis captureSavedAnalysis() {
        ArgumentCaptor<ErrorAnalysis> captor = ArgumentCaptor.forClass(ErrorAnalysis.class);
        verify(errorAnalysisRepository).save(captor.capture());
        return captor.getValue();
    }

    @Nested
    @DisplayName("확신도 임계값")
    class ConfidenceThreshold {

        @Test
        @DisplayName("임계값 미만 후보는 낮은 확신도로 표시되어 아동 교정 대상에서 빠진다")
        void separatesLowConfidence() {
            givenNewAnalysis();

            service.completeAnalysis(WRITING_ID, PROFILE_ID, "stub", new AiErrorAnalysisResponse(List.of(
                    new ErrorItem("FINAL_CONSONANT", 12, 15, "놀앗다", "놀았다", 0.93, "받침 표기"),
                    new ErrorItem("SPACING", 3, 7, "학교에서", "학교 에서", 0.58, "구어체"))));

            ErrorAnalysis saved = captureSavedAnalysis();
            assertThat(saved.confirmedCandidates())
                    .singleElement()
                    .satisfies(candidate -> {
                        assertThat(candidate.getErrorType()).isEqualTo(ErrorType.FINAL_CONSONANT);
                        assertThat(candidate.isLowConfidence()).isFalse();
                    });
            assertThat(saved.reviewCandidates())
                    .singleElement()
                    .satisfies(candidate -> assertThat(candidate.getErrorType()).isEqualTo(ErrorType.SPACING));
        }

        @Test
        @DisplayName("확신도가 임계값과 같으면 확정 오류로 본다")
        void treatsThresholdAsConfirmed() {
            givenNewAnalysis();

            service.completeAnalysis(WRITING_ID, PROFILE_ID, "stub", new AiErrorAnalysisResponse(List.of(
                    new ErrorItem("SPELLING", 0, 2, "안돼", "안 돼", THRESHOLD, null))));

            assertThat(captureSavedAnalysis().confirmedCandidates()).hasSize(1);
        }

        @Test
        @DisplayName("확신도가 없으면 0 으로 보아 검토 대상으로 분리한다")
        void treatsNullConfidenceAsLowest() {
            givenNewAnalysis();

            service.completeAnalysis(WRITING_ID, PROFILE_ID, "stub", new AiErrorAnalysisResponse(List.of(
                    new ErrorItem("VOCABULARY", 0, 2, "되게", "매우", null, null))));

            assertThat(captureSavedAnalysis().confirmedCandidates()).isEmpty();
        }
    }

    @Nested
    @DisplayName("반복 오류 프로필")
    class Profile {

        @Test
        @DisplayName("확정 오류만 누적한다 — 낮은 확신도 후보는 집계하지 않는다")
        void aggregatesConfirmedOnly() {
            givenNewAnalysis();
            given(errorProfileRepository.findByProfileIdAndErrorType(eq(PROFILE_ID), any(ErrorType.class)))
                    .willReturn(Optional.empty());
            given(errorProfileRepository.save(any(ErrorProfile.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            service.completeAnalysis(WRITING_ID, PROFILE_ID, "stub", new AiErrorAnalysisResponse(List.of(
                    new ErrorItem("SPACING", 0, 2, "집에왔다", "집에 왔다", 0.91, null),
                    new ErrorItem("SPACING", 5, 8, "학교에갔다", "학교에 갔다", 0.88, null),
                    // 임계값 미만. 프로필에 반영되면 안 된다.
                    new ErrorItem("SPELLING", 9, 11, "되게", "매우", 0.40, null))));

            ArgumentCaptor<ErrorProfile> captor = ArgumentCaptor.forClass(ErrorProfile.class);
            verify(errorProfileRepository).save(captor.capture());

            ErrorProfile profile = captor.getValue();
            assertThat(profile.getErrorType()).isEqualTo(ErrorType.SPACING);
            assertThat(profile.getOccurrenceCount()).isEqualTo(2);
            assertThat(profile.getLastOccurredOn()).isNotNull();
        }

        @Test
        @DisplayName("확정 오류가 하나도 없으면 프로필을 건드리지 않는다")
        void skipsWhenNoConfirmed() {
            givenNewAnalysis();

            service.completeAnalysis(WRITING_ID, PROFILE_ID, "stub", new AiErrorAnalysisResponse(List.of(
                    new ErrorItem("SPACING", 0, 2, "집에왔다", "집에 왔다", 0.30, null))));

            verify(errorProfileRepository, never()).save(any(ErrorProfile.class));
        }
    }

    @Nested
    @DisplayName("AI 응답 방어")
    class Resilience {

        @Test
        @DisplayName("모르는 오류 유형은 건너뛰고 나머지는 저장한다")
        void skipsUnknownErrorType() {
            givenNewAnalysis();
            given(errorProfileRepository.findByProfileIdAndErrorType(eq(PROFILE_ID), any(ErrorType.class)))
                    .willReturn(Optional.empty());
            given(errorProfileRepository.save(any(ErrorProfile.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            service.completeAnalysis(WRITING_ID, PROFILE_ID, "stub", new AiErrorAnalysisResponse(List.of(
                    new ErrorItem("PUNCTUATION", 0, 1, ".", ",", 0.99, null),
                    new ErrorItem("SPACING", 5, 8, "학교에갔다", "학교에 갔다", 0.88, null))));

            assertThat(captureSavedAnalysis().getCandidates())
                    .singleElement()
                    .satisfies(candidate -> assertThat(candidate.getErrorType()).isEqualTo(ErrorType.SPACING));
        }

        @Test
        @DisplayName("오류 목록이 null 이어도 성공으로 처리한다")
        void handlesNullErrors() {
            givenNewAnalysis();

            service.completeAnalysis(WRITING_ID, PROFILE_ID, "stub", new AiErrorAnalysisResponse(null));

            assertThat(captureSavedAnalysis().getCandidates()).isEmpty();
        }
    }

    @Nested
    @DisplayName("실패 처리")
    class Failure {

        @Test
        @DisplayName("실패 상태와 사유를 남겨 재처리할 수 있게 한다")
        void recordsFailure() {
            givenNewAnalysis();

            service.failAnalysis(WRITING_ID, PROFILE_ID, "connection refused");

            ErrorAnalysis saved = captureSavedAnalysis();
            assertThat(saved.isFailed()).isTrue();
            assertThat(saved.getFailureReason()).isEqualTo("connection refused");
            verify(activityEventService).record(
                    eq(ActivityEventType.ERROR_ANALYSIS_FAILED), eq(PROFILE_ID), eq(WRITING_ID), any());
        }
    }
}
