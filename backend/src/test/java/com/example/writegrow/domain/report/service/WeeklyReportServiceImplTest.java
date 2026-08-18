package com.example.writegrow.domain.report.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.writegrow.domain.account.exception.AccountErrorCode;
import com.example.writegrow.domain.account.exception.AccountException;
import com.example.writegrow.domain.account.repository.ProfileRepository;
import com.example.writegrow.domain.account.service.ProfileService;
import com.example.writegrow.domain.activity.entity.ActivityEventType;
import com.example.writegrow.domain.activity.service.ActivityEventService;
import com.example.writegrow.domain.analysis.entity.ErrorAnalysis;
import com.example.writegrow.domain.analysis.entity.ErrorProfile;
import com.example.writegrow.domain.analysis.entity.ErrorType;
import com.example.writegrow.domain.analysis.repository.ErrorAnalysisRepository;
import com.example.writegrow.domain.analysis.repository.ErrorProfileRepository;
import com.example.writegrow.domain.report.dto.response.WeeklyReportResponse;
import com.example.writegrow.domain.writing.entity.Writing;
import com.example.writegrow.domain.writing.repository.WritingRepository;
import com.example.writegrow.support.AccountFixtures;
import com.example.writegrow.support.WritingFixtures;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@DisplayName("WeeklyReportServiceImpl 단위 테스트")
class WeeklyReportServiceImplTest {

    private static final Long CHILD_ID = 1L;
    private static final Long PARENT_ID = 2L;

    @Mock
    private ProfileService profileService;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private WritingRepository writingRepository;

    @Mock
    private ErrorAnalysisRepository errorAnalysisRepository;

    @Mock
    private ErrorProfileRepository errorProfileRepository;

    @Mock
    private ActivityEventService activityEventService;

    private WeeklyReportServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new WeeklyReportServiceImpl(
                profileService, profileRepository, writingRepository,
                errorAnalysisRepository, errorProfileRepository, activityEventService);
    }

    /**
     * 확정 오류 {@code confirmed} 건과 낮은 확신도 후보 {@code review} 건을 가진 분석 결과.
     */
    private ErrorAnalysis analysis(int confirmed, int review, LocalDateTime completedAt) {
        ErrorAnalysis analysis = ErrorAnalysis.create(100L, CHILD_ID);
        analysis.markProcessing("오늘 학교에서 친구랑 놀앗다");
        int seq = 0;
        for (int i = 0; i < confirmed; i++) {
            analysis.addCandidate(seq++, ErrorType.FINAL_CONSONANT, 0, 1, "앗", "았", 0.95, null, false);
        }
        for (int i = 0; i < review; i++) {
            analysis.addCandidate(seq++, ErrorType.SPACING, 0, 1, "에서", "에 서", 0.40, null, true);
        }
        analysis.markSucceeded("stub");
        ReflectionTestUtils.setField(analysis, "completedAt", completedAt);
        return analysis;
    }

    private Writing writingCreatedAt(Long id, LocalDateTime createdAt, boolean selfCorrected) {
        Writing writing = WritingFixtures.penWriting(id, CHILD_ID);
        writing.submitHandwriting();
        writing.applyOcrText("오늘 학교에서 친구랑 놀앗다");
        if (selfCorrected) {
            writing.confirmText("오늘 학교에서 친구랑 놀았다");
        } else {
            writing.confirmText("오늘 학교에서 친구랑 놀앗다");
        }
        ReflectionTestUtils.setField(writing, "createdAt", createdAt);
        return writing;
    }

    private void givenChildViewable() {
        given(profileService.getViewableChild(PARENT_ID, CHILD_ID))
                .willReturn(AccountFixtures.childProfile(CHILD_ID));
    }

    @Nested
    @DisplayName("주간 리포트")
    class Report {

        @Test
        @DisplayName("낮은 확신도 후보는 오류 집계에서 빠지고 검토 대기 건수로만 잡힌다")
        void excludesLowConfidenceFromCounts() {
            givenChildViewable();
            LocalDateTime now = LocalDateTime.now();
            given(writingRepository.findAllInPeriod(eq(CHILD_ID), any(), any()))
                    .willReturn(List.of(writingCreatedAt(10L, now, true)));
            given(errorAnalysisRepository.findAllWithCandidatesInPeriod(eq(CHILD_ID), any(), any()))
                    .willReturn(List.of(analysis(2, 3, now)));
            given(errorProfileRepository.findAllByProfileIdOrderByOccurrenceCountDesc(CHILD_ID))
                    .willReturn(List.of());

            WeeklyReportResponse report = service.getWeeklyReport(PARENT_ID, CHILD_ID, LocalDate.now());

            assertThat(report.summary().confirmedErrorCount()).isEqualTo(2);
            assertThat(report.summary().reviewPendingCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("자기교정은 아동이 직접 고친 기록으로 센다")
        void countsSelfCorrectionFromChildEdits() {
            givenChildViewable();
            LocalDateTime now = LocalDateTime.now();
            given(writingRepository.findAllInPeriod(eq(CHILD_ID), any(), any()))
                    .willReturn(List.of(
                            writingCreatedAt(10L, now, true),
                            writingCreatedAt(11L, now, false)));
            given(errorAnalysisRepository.findAllWithCandidatesInPeriod(eq(CHILD_ID), any(), any()))
                    .willReturn(List.of());
            given(errorProfileRepository.findAllByProfileIdOrderByOccurrenceCountDesc(CHILD_ID))
                    .willReturn(List.of());

            WeeklyReportResponse report = service.getWeeklyReport(PARENT_ID, CHILD_ID, LocalDate.now());

            assertThat(report.summary().writingCount()).isEqualTo(2);
            assertThat(report.summary().selfCorrectionCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("작성한 글이 없으면 작성 공백을 알리고 집중 영역을 만들지 않는다")
        void marksEmptyWeek() {
            givenChildViewable();
            given(writingRepository.findAllInPeriod(eq(CHILD_ID), any(), any())).willReturn(List.of());
            given(errorAnalysisRepository.findAllWithCandidatesInPeriod(eq(CHILD_ID), any(), any()))
                    .willReturn(List.of());
            given(errorProfileRepository.findAllByProfileIdOrderByOccurrenceCountDesc(CHILD_ID))
                    .willReturn(List.of());

            WeeklyReportResponse report = service.getWeeklyReport(PARENT_ID, CHILD_ID, LocalDate.now());

            assertThat(report.hasWriting()).isFalse();
            assertThat(report.nextFocus()).isNull();
        }

        @Test
        @DisplayName("일자별 추이는 글이 없는 날도 채워 일곱 칸을 돌려준다")
        void fillsSevenDays() {
            givenChildViewable();
            given(writingRepository.findAllInPeriod(eq(CHILD_ID), any(), any())).willReturn(List.of());
            given(errorAnalysisRepository.findAllWithCandidatesInPeriod(eq(CHILD_ID), any(), any()))
                    .willReturn(List.of());
            given(errorProfileRepository.findAllByProfileIdOrderByOccurrenceCountDesc(CHILD_ID))
                    .willReturn(List.of());

            WeeklyReportResponse report = service.getWeeklyReport(PARENT_ID, CHILD_ID, LocalDate.now());

            assertThat(report.dailyTrends()).hasSize(7);
            assertThat(report.dailyTrends().get(0).date()).isEqualTo(report.weekStart());
            assertThat(report.dailyTrends().get(6).date()).isEqualTo(report.weekEnd());
        }

        @Test
        @DisplayName("가장 많이 반복된 유형을 집중 영역으로 고른다")
        void picksMostRepeatedAsFocus() {
            givenChildViewable();
            given(writingRepository.findAllInPeriod(eq(CHILD_ID), any(), any())).willReturn(List.of());
            given(errorAnalysisRepository.findAllWithCandidatesInPeriod(eq(CHILD_ID), any(), any()))
                    .willReturn(List.of());

            ErrorProfile spacing = ErrorProfile.create(CHILD_ID, ErrorType.SPACING);
            spacing.recordOccurrences(12, LocalDate.now());
            ErrorProfile finalConsonant = ErrorProfile.create(CHILD_ID, ErrorType.FINAL_CONSONANT);
            finalConsonant.recordOccurrences(4, LocalDate.now());
            given(errorProfileRepository.findAllByProfileIdOrderByOccurrenceCountDesc(CHILD_ID))
                    .willReturn(List.of(spacing, finalConsonant));

            WeeklyReportResponse report = service.getWeeklyReport(PARENT_ID, CHILD_ID, LocalDate.now());

            assertThat(report.nextFocus().errorType()).isEqualTo(ErrorType.SPACING);
            assertThat(report.nextFocus().basisValue()).isEqualTo(12);
            assertThat(report.summary().repeatedErrorTypeCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("열람 이벤트를 기록한다")
        void recordsViewEvent() {
            givenChildViewable();
            given(writingRepository.findAllInPeriod(eq(CHILD_ID), any(), any())).willReturn(List.of());
            given(errorAnalysisRepository.findAllWithCandidatesInPeriod(eq(CHILD_ID), any(), any()))
                    .willReturn(List.of());
            given(errorProfileRepository.findAllByProfileIdOrderByOccurrenceCountDesc(CHILD_ID))
                    .willReturn(List.of());

            service.getWeeklyReport(PARENT_ID, CHILD_ID, LocalDate.now());

            verify(activityEventService).record(
                    eq(ActivityEventType.WEEKLY_REPORT_VIEWED), eq(PARENT_ID), isNull(), any());
        }

        @Test
        @DisplayName("연결되지 않은 아동은 조회할 수 없다")
        void rejectsUnlinkedChild() {
            willThrow(new AccountException(AccountErrorCode.NOT_LINKED_CHILD))
                    .given(profileService).getViewableChild(PARENT_ID, CHILD_ID);

            assertThatThrownBy(() -> service.getWeeklyReport(PARENT_ID, CHILD_ID, LocalDate.now()))
                    .isInstanceOf(AccountException.class)
                    .extracting(exception -> ((AccountException) exception).getErrorCode())
                    .isEqualTo(AccountErrorCode.NOT_LINKED_CHILD);

            verify(writingRepository, never()).findAllInPeriod(anyLong(), any(), any());
        }
    }
}
