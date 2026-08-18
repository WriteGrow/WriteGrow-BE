package com.example.writegrow.domain.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.writegrow.domain.account.exception.AccountErrorCode;
import com.example.writegrow.domain.account.exception.AccountException;
import com.example.writegrow.domain.account.service.ProfileService;
import com.example.writegrow.domain.activity.entity.ActivityEventType;
import com.example.writegrow.domain.activity.service.ActivityEventService;
import com.example.writegrow.domain.analysis.dto.response.ErrorReviewResponse;
import com.example.writegrow.domain.analysis.dto.response.WritingErrorsResponse;
import com.example.writegrow.domain.analysis.entity.ErrorAnalysis;
import com.example.writegrow.domain.analysis.entity.ErrorType;
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
import com.example.writegrow.support.WritingFixtures;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ErrorAnalysisServiceImpl 단위 테스트")
class ErrorAnalysisServiceImplTest {

    private static final Long WRITING_ID = 100L;
    private static final Long CHILD_ID = 1L;
    private static final Long PARENT_ID = 2L;
    private static final String CONFIRMED_TEXT = "오늘 학교에서 친구랑 놀았다";

    @Mock
    private ErrorAnalysisResultService errorAnalysisResultService;

    @Mock
    private ErrorAnalysisRepository errorAnalysisRepository;

    @Mock
    private ErrorProfileRepository errorProfileRepository;

    @Mock
    private WritingRepository writingRepository;

    @Mock
    private ProfileService profileService;

    @Mock
    private ActivityEventService activityEventService;

    @Mock
    private AiAnalysisClient aiAnalysisClient;

    private ErrorAnalysisServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ErrorAnalysisServiceImpl(
                errorAnalysisResultService, errorAnalysisRepository, errorProfileRepository,
                writingRepository, profileService, activityEventService, aiAnalysisClient);
    }

    private Writing confirmedWriting() {
        Writing writing = WritingFixtures.keyboardWriting(WRITING_ID, CHILD_ID);
        writing.submitKeyboard(CONFIRMED_TEXT);
        return writing;
    }

    private ErrorAnalysis analysisWithBothKinds() {
        ErrorAnalysis analysis = ErrorAnalysis.create(WRITING_ID, CHILD_ID);
        analysis.markProcessing(CONFIRMED_TEXT);
        analysis.addCandidate(0, ErrorType.FINAL_CONSONANT, 12, 15, "놀앗다", "놀았다", 0.93, "받침", false);
        analysis.addCandidate(1, ErrorType.SPACING, 3, 7, "학교에서", "학교 에서", 0.58, "구어체", true);
        analysis.markSucceeded("stub");
        return analysis;
    }

    @Nested
    @DisplayName("분석 실행")
    class RunAnalysis {

        @Test
        @DisplayName("확정 텍스트를 AI 에 보내고 결과를 저장한다")
        void analyzesConfirmedText() {
            given(writingRepository.findById(WRITING_ID)).willReturn(Optional.of(confirmedWriting()));
            given(aiAnalysisClient.analyzeText(any(AiErrorAnalysisRequest.class)))
                    .willReturn(new AiErrorAnalysisResponse(List.of()));
            given(aiAnalysisClient.provider()).willReturn("stub");

            service.runAnalysis(WRITING_ID, CHILD_ID);

            verify(errorAnalysisResultService).markProcessing(WRITING_ID, CHILD_ID, CONFIRMED_TEXT);
            verify(errorAnalysisResultService).completeAnalysis(
                    eq(WRITING_ID), eq(CHILD_ID), eq("stub"), any(AiErrorAnalysisResponse.class));
        }

        @Test
        @DisplayName("확정 텍스트가 없으면 분석을 시작하지 않는다")
        void skipsWithoutConfirmedText() {
            // 아직 제출 전이라 finalText 가 없다. 선행조건 미충족이므로 실패로 남기지 않는다.
            given(writingRepository.findById(WRITING_ID))
                    .willReturn(Optional.of(WritingFixtures.keyboardWriting(WRITING_ID, CHILD_ID)));

            service.runAnalysis(WRITING_ID, CHILD_ID);

            verify(errorAnalysisResultService, never()).markProcessing(anyLong(), anyLong(), any());
            verify(errorAnalysisResultService, never()).failAnalysis(anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("AI 호출이 실패하면 실패 상태로 기록한다")
        void recordsFailure() {
            given(writingRepository.findById(WRITING_ID)).willReturn(Optional.of(confirmedWriting()));
            willThrow(new IllegalStateException("connection refused"))
                    .given(aiAnalysisClient).analyzeText(any(AiErrorAnalysisRequest.class));

            service.runAnalysis(WRITING_ID, CHILD_ID);

            verify(errorAnalysisResultService).failAnalysis(WRITING_ID, CHILD_ID, "connection refused");
        }
    }

    @Nested
    @DisplayName("아동 조회")
    class ConfirmedErrors {

        @Test
        @DisplayName("확정 오류만 내려주고 낮은 확신도 후보는 개수도 노출하지 않는다")
        void returnsConfirmedOnly() {
            given(writingRepository.findById(WRITING_ID)).willReturn(Optional.of(confirmedWriting()));
            given(errorAnalysisRepository.findWithCandidatesByWritingId(WRITING_ID))
                    .willReturn(Optional.of(analysisWithBothKinds()));

            WritingErrorsResponse response = service.getConfirmedErrors(CHILD_ID, WRITING_ID);

            assertThat(response.errors())
                    .singleElement()
                    .satisfies(error -> assertThat(error.errorType()).isEqualTo(ErrorType.FINAL_CONSONANT));
        }

        @Test
        @DisplayName("다른 아동의 글은 조회할 수 없다")
        void rejectsOtherProfile() {
            given(writingRepository.findById(WRITING_ID))
                    .willReturn(Optional.of(WritingFixtures.keyboardWriting(WRITING_ID, 999L)));

            assertThatThrownBy(() -> service.getConfirmedErrors(CHILD_ID, WRITING_ID))
                    .isInstanceOf(WritingException.class)
                    .extracting(exception -> ((WritingException) exception).getErrorCode())
                    .isEqualTo(WritingErrorCode.FORBIDDEN_PROFILE);
        }

        @Test
        @DisplayName("분석 결과가 아직 없으면 ANALYSIS_NOT_FOUND")
        void rejectsMissingAnalysis() {
            given(writingRepository.findById(WRITING_ID)).willReturn(Optional.of(confirmedWriting()));
            given(errorAnalysisRepository.findWithCandidatesByWritingId(WRITING_ID))
                    .willReturn(Optional.empty());

            assertThatThrownBy(() -> service.getConfirmedErrors(CHILD_ID, WRITING_ID))
                    .isInstanceOf(AnalysisException.class)
                    .extracting(exception -> ((AnalysisException) exception).getErrorCode())
                    .isEqualTo(AnalysisErrorCode.ANALYSIS_NOT_FOUND);
        }
    }

    @Nested
    @DisplayName("보호자 검토 조회")
    class ReviewCandidates {

        @Test
        @DisplayName("낮은 확신도 후보와 확정 오류 개수를 함께 내려주고 열람 이벤트를 남긴다")
        void returnsReviewCandidates() {
            given(writingRepository.findById(WRITING_ID)).willReturn(Optional.of(confirmedWriting()));
            given(errorAnalysisRepository.findWithCandidatesByWritingId(WRITING_ID))
                    .willReturn(Optional.of(analysisWithBothKinds()));

            ErrorReviewResponse response = service.getReviewCandidates(PARENT_ID, WRITING_ID);

            assertThat(response.reviewCount()).isEqualTo(1);
            assertThat(response.confirmedCount()).isEqualTo(1);
            assertThat(response.reviewCandidates())
                    .singleElement()
                    .satisfies(candidate -> assertThat(candidate.errorType()).isEqualTo(ErrorType.SPACING));

            verify(profileService).getViewableChild(PARENT_ID, CHILD_ID);
            verify(activityEventService).record(
                    ActivityEventType.ERROR_REVIEW_VIEWED, PARENT_ID, WRITING_ID);
        }

        @Test
        @DisplayName("연결되지 않은 아동의 글은 조회할 수 없다")
        void rejectsUnlinkedChild() {
            given(writingRepository.findById(WRITING_ID)).willReturn(Optional.of(confirmedWriting()));
            willThrow(new AccountException(AccountErrorCode.NOT_LINKED_CHILD))
                    .given(profileService).getViewableChild(PARENT_ID, CHILD_ID);

            assertThatThrownBy(() -> service.getReviewCandidates(PARENT_ID, WRITING_ID))
                    .isInstanceOf(AccountException.class)
                    .extracting(exception -> ((AccountException) exception).getErrorCode())
                    .isEqualTo(AccountErrorCode.NOT_LINKED_CHILD);
        }
    }

    @Nested
    @DisplayName("반복 오류 프로필 조회")
    class Profile {

        @Test
        @DisplayName("권한을 확인한 뒤 유형별 지표를 돌려준다")
        void returnsProfile() {
            given(errorProfileRepository.findAllByProfileIdOrderByOccurrenceCountDesc(CHILD_ID))
                    .willReturn(List.of());

            var response = service.getErrorProfile(PARENT_ID, CHILD_ID);

            assertThat(response.profileId()).isEqualTo(CHILD_ID);
            verify(profileService).getViewableChild(PARENT_ID, CHILD_ID);
        }
    }
}
