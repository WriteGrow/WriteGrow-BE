package com.example.writegrow.domain.writing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.writegrow.domain.account.service.ProfileService;
import com.example.writegrow.domain.activity.entity.ActivityEventType;
import com.example.writegrow.domain.activity.service.ActivityEventService;
import com.example.writegrow.domain.handwriting.service.HandwritingQueryService;
import com.example.writegrow.domain.writing.dto.request.WritingCreateRequest;
import com.example.writegrow.domain.writing.dto.request.WritingSubmitRequest;
import com.example.writegrow.domain.writing.dto.request.WritingTextConfirmRequest;
import com.example.writegrow.domain.writing.dto.response.WritingCreateResponse;
import com.example.writegrow.domain.writing.dto.response.WritingDetailResponse;
import com.example.writegrow.domain.writing.dto.response.TodayWritingStatusResponse;
import com.example.writegrow.domain.writing.dto.response.WritingSubmitResponse;
import com.example.writegrow.domain.writing.dto.response.WritingSummaryResponse;
import com.example.writegrow.domain.writing.dto.response.WritingTextConfirmResponse;
import com.example.writegrow.domain.writing.entity.InputType;
import com.example.writegrow.domain.writing.entity.Writing;
import com.example.writegrow.domain.writing.entity.WritingStatus;
import com.example.writegrow.domain.writing.event.HandwritingSubmittedEvent;
import com.example.writegrow.domain.writing.event.TextConfirmedEvent;
import com.example.writegrow.domain.writing.exception.WritingErrorCode;
import com.example.writegrow.domain.writing.exception.WritingException;
import com.example.writegrow.domain.writing.repository.WritingRepository;
import com.example.writegrow.global.common.PageResponse;
import com.example.writegrow.global.config.properties.WritingProperties;
import com.example.writegrow.support.AccountFixtures;
import com.example.writegrow.support.WritingFixtures;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
@DisplayName("WritingServiceImpl 단위 테스트")
class WritingServiceImplTest {

    private static final Long PROFILE_ID = 1L;
    private static final Long OTHER_PROFILE_ID = 2L;
    private static final Long WRITING_ID = 100L;

    @Mock
    private WritingRepository writingRepository;

    @Mock
    private ProfileService profileService;

    @Mock
    private HandwritingQueryService handwritingQueryService;

    @Mock
    private ActivityEventService activityEventService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    private WritingServiceImpl writingService;

    @BeforeEach
    void setUp() {
        // 하루 목표는 설정값이라 목이 아니라 실제 값을 넣는다. 목표 달성 판정이 검증 대상이다.
        writingService = new WritingServiceImpl(
                writingRepository, profileService, handwritingQueryService,
                activityEventService, eventPublisher, new WritingProperties(1));
    }

    @Nested
    @DisplayName("글쓰기 시작")
    class Create {

        @Test
        @DisplayName("보호자 동의가 확인된 아동만 글을 시작할 수 있다")
        void checksWritableChild() {
            given(profileService.getWritableChild(PROFILE_ID))
                    .willReturn(AccountFixtures.childProfile(PROFILE_ID));
            given(writingRepository.save(any(Writing.class))).willAnswer(invocation -> {
                Writing writing = invocation.getArgument(0);
                WritingFixtures.setId(writing, WRITING_ID);
                return writing;
            });

            WritingCreateResponse response = writingService.create(
                    PROFILE_ID, new WritingCreateRequest(InputType.PEN, "오늘 있었던 일"));

            assertThat(response.writingId()).isEqualTo(WRITING_ID);
            assertThat(response.status()).isEqualTo(WritingStatus.DRAFT);
            verify(profileService).getWritableChild(PROFILE_ID);
        }
    }

    @Nested
    @DisplayName("글 제출")
    class Submit {

        @Test
        @DisplayName("키보드 글은 즉시 확정되고 손글씨 분석 대신 오류 분석 이벤트를 발행한다")
        void submitsKeyboardWriting() {
            Writing writing = WritingFixtures.keyboardWriting(WRITING_ID, PROFILE_ID);
            given(writingRepository.findWithRevisionsById(WRITING_ID)).willReturn(Optional.of(writing));

            WritingSubmitResponse response = writingService.submit(
                    PROFILE_ID, WRITING_ID, new WritingSubmitRequest("오늘 학교에서 친구랑 놀았다"));

            assertThat(response.status()).isEqualTo(WritingStatus.CONFIRMED);
            assertThat(response.analysisInProgress()).isFalse();
            verify(eventPublisher, never()).publishEvent(any(HandwritingSubmittedEvent.class));
            // 최종본이 확정됐으므로 오류 분석(REQ-03)이 시작되어야 한다.
            verify(eventPublisher).publishEvent(new TextConfirmedEvent(WRITING_ID, PROFILE_ID));
            verify(activityEventService).record(
                    eq(ActivityEventType.WRITING_SUBMITTED), eq(PROFILE_ID), eq(WRITING_ID), any());
            verify(activityEventService).record(
                    eq(ActivityEventType.WRITING_CONFIRMED), eq(PROFILE_ID), eq(WRITING_ID), any());
        }

        @Test
        @DisplayName("손글씨 글은 분석 대기 상태가 되고 분석 시작 이벤트를 발행한다")
        void submitsHandwriting() {
            Writing writing = WritingFixtures.penWriting(WRITING_ID, PROFILE_ID);
            given(writingRepository.findWithRevisionsById(WRITING_ID)).willReturn(Optional.of(writing));

            WritingSubmitResponse response = writingService.submit(PROFILE_ID, WRITING_ID, null);

            assertThat(response.status()).isEqualTo(WritingStatus.SUBMITTED);
            assertThat(response.analysisInProgress()).isTrue();

            ArgumentCaptor<HandwritingSubmittedEvent> captor =
                    ArgumentCaptor.forClass(HandwritingSubmittedEvent.class);
            verify(eventPublisher).publishEvent(captor.capture());
            assertThat(captor.getValue().writingId()).isEqualTo(WRITING_ID);
            assertThat(captor.getValue().profileId()).isEqualTo(PROFILE_ID);

            verify(activityEventService, never()).record(
                    eq(ActivityEventType.WRITING_CONFIRMED), anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("키보드 글의 내용이 비어 있으면 EMPTY_CONTENT 예외가 발생한다")
        void rejectsEmptyKeyboardContent() {
            Writing writing = WritingFixtures.keyboardWriting(WRITING_ID, PROFILE_ID);
            given(writingRepository.findWithRevisionsById(WRITING_ID)).willReturn(Optional.of(writing));

            assertThatThrownBy(() -> writingService.submit(
                    PROFILE_ID, WRITING_ID, new WritingSubmitRequest("   ")))
                    .isInstanceOf(WritingException.class)
                    .extracting(exception -> ((WritingException) exception).getErrorCode())
                    .isEqualTo(WritingErrorCode.EMPTY_CONTENT);
        }

        @Test
        @DisplayName("다른 아동의 글은 제출할 수 없다")
        void rejectsOtherProfile() {
            Writing writing = WritingFixtures.keyboardWriting(WRITING_ID, OTHER_PROFILE_ID);
            given(writingRepository.findWithRevisionsById(WRITING_ID)).willReturn(Optional.of(writing));

            assertThatThrownBy(() -> writingService.submit(
                    PROFILE_ID, WRITING_ID, new WritingSubmitRequest("남의 글")))
                    .isInstanceOf(WritingException.class)
                    .extracting(exception -> ((WritingException) exception).getErrorCode())
                    .isEqualTo(WritingErrorCode.FORBIDDEN_PROFILE);
        }
    }

    @Nested
    @DisplayName("조회")
    class Read {

        @Test
        @DisplayName("존재하지 않는 글이면 WRITING_NOT_FOUND 예외가 발생한다")
        void throwsWhenMissing() {
            given(writingRepository.findWithRevisionsById(WRITING_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> writingService.getWriting(PROFILE_ID, WRITING_ID))
                    .isInstanceOf(WritingException.class)
                    .extracting(exception -> ((WritingException) exception).getErrorCode())
                    .isEqualTo(WritingErrorCode.WRITING_NOT_FOUND);
        }

        @Test
        @DisplayName("키보드 글 상세에는 손글씨 정보가 없다")
        void detailWithoutHandwriting() {
            Writing writing = WritingFixtures.keyboardWriting(WRITING_ID, PROFILE_ID);
            writing.submitKeyboard("오늘 학교에서 친구랑 놀았다");
            given(writingRepository.findWithRevisionsById(WRITING_ID)).willReturn(Optional.of(writing));
            given(handwritingQueryService.findByWritingId(WRITING_ID)).willReturn(Optional.empty());

            WritingDetailResponse response = writingService.getWriting(PROFILE_ID, WRITING_ID);

            assertThat(response.handwriting()).isNull();
            assertThat(response.finalText()).isEqualTo("오늘 학교에서 친구랑 놀았다");
            assertThat(response.revisions()).hasSize(1);
        }

        @Test
        @DisplayName("목록은 최종 수정본 미리보기를 포함해 페이지로 변환된다")
        void listsWritings() {
            Writing writing = WritingFixtures.keyboardWriting(WRITING_ID, PROFILE_ID);
            writing.submitKeyboard("오늘 학교에서 친구랑 놀았다");
            Pageable pageable = PageRequest.of(0, 20);
            given(writingRepository.findAllByProfileId(PROFILE_ID, pageable))
                    .willReturn(new PageImpl<>(List.of(writing), pageable, 1));

            PageResponse<WritingSummaryResponse> response = writingService.getWritings(PROFILE_ID, pageable);

            assertThat(response.totalElements()).isEqualTo(1);
            assertThat(response.content()).hasSize(1);
            assertThat(response.content().getFirst().preview()).isEqualTo("오늘 학교에서 친구랑 놀았다");
        }
    }

    @Nested
    @DisplayName("최종본 확정")
    class ConfirmText {

        @Test
        @DisplayName("아동이 텍스트를 고치면 수정 이벤트와 확정 이벤트를 모두 기록한다")
        void recordsEditEvent() {
            Writing writing = WritingFixtures.analyzedPenWriting(WRITING_ID, PROFILE_ID, "오늘 학교에서 친구랑 놀앗다");
            given(writingRepository.findWithRevisionsById(WRITING_ID)).willReturn(Optional.of(writing));

            WritingTextConfirmResponse response = writingService.confirmText(
                    PROFILE_ID, WRITING_ID, new WritingTextConfirmRequest("오늘 학교에서 친구랑 놀았다"));

            assertThat(response.edited()).isTrue();
            assertThat(response.status()).isEqualTo(WritingStatus.CONFIRMED);
            verify(activityEventService).record(
                    eq(ActivityEventType.OCR_TEXT_EDITED), eq(PROFILE_ID), eq(WRITING_ID), any());
            verify(activityEventService).record(
                    ActivityEventType.WRITING_CONFIRMED, PROFILE_ID, WRITING_ID, Map.of("edited", true));
        }

        @Test
        @DisplayName("고치지 않고 확정하면 수정 이벤트는 기록하지 않는다")
        void skipsEditEventWhenUnchanged() {
            Writing writing = WritingFixtures.analyzedPenWriting(WRITING_ID, PROFILE_ID, "오늘 학교에서 친구랑 놀았다");
            given(writingRepository.findWithRevisionsById(WRITING_ID)).willReturn(Optional.of(writing));

            WritingTextConfirmResponse response = writingService.confirmText(
                    PROFILE_ID, WRITING_ID, new WritingTextConfirmRequest("오늘 학교에서 친구랑 놀았다"));

            assertThat(response.edited()).isFalse();
            verify(activityEventService, never()).record(
                    eq(ActivityEventType.OCR_TEXT_EDITED), anyLong(), anyLong(), any());
        }

        @Test
        @DisplayName("확정하면 오류 분석 이벤트를 발행한다")
        void publishesTextConfirmedEvent() {
            Writing writing = WritingFixtures.analyzedPenWriting(WRITING_ID, PROFILE_ID, "오늘 학교에서 친구랑 놀앗다");
            given(writingRepository.findWithRevisionsById(WRITING_ID)).willReturn(Optional.of(writing));

            writingService.confirmText(
                    PROFILE_ID, WRITING_ID, new WritingTextConfirmRequest("오늘 학교에서 친구랑 놀았다"));

            verify(eventPublisher).publishEvent(new TextConfirmedEvent(WRITING_ID, PROFILE_ID));
        }
    }

    @Nested
    @DisplayName("오늘 작성 현황")
    class TodayStatus {

        @Test
        @DisplayName("오늘 쓴 글이 없으면 목표 미달성으로 응답한다")
        void reportsEmptyDay() {
            given(writingRepository.findAllInPeriod(eq(PROFILE_ID), any(), any())).willReturn(List.of());

            TodayWritingStatusResponse response = writingService.getTodayStatus(PROFILE_ID);

            assertThat(response.writingCount()).isZero();
            assertThat(response.dailyGoal()).isEqualTo(1);
            assertThat(response.goalAchieved()).isFalse();
            assertThat(response.inProgressWritingId()).isNull();
        }

        @Test
        @DisplayName("목표만큼 썼으면 달성으로 표시한다")
        void marksGoalAchieved() {
            Writing writing = WritingFixtures.keyboardWriting(WRITING_ID, PROFILE_ID);
            writing.submitKeyboard("오늘 학교에서 친구랑 놀았다");
            given(writingRepository.findAllInPeriod(eq(PROFILE_ID), any(), any()))
                    .willReturn(List.of(writing));

            TodayWritingStatusResponse response = writingService.getTodayStatus(PROFILE_ID);

            assertThat(response.writingCount()).isEqualTo(1);
            assertThat(response.goalAchieved()).isTrue();
            // 제출을 마친 글은 이어쓸 대상이 아니다.
            assertThat(response.inProgressWritingId()).isNull();
        }

        @Test
        @DisplayName("작성 중인 글이 있으면 이어쓸 글 ID 를 알려준다")
        void exposesInProgressWriting() {
            given(writingRepository.findAllInPeriod(eq(PROFILE_ID), any(), any()))
                    .willReturn(List.of(WritingFixtures.keyboardWriting(WRITING_ID, PROFILE_ID)));

            TodayWritingStatusResponse response = writingService.getTodayStatus(PROFILE_ID);

            assertThat(response.inProgressWritingId()).isEqualTo(WRITING_ID);
        }
    }

    @Nested
    @DisplayName("다시 쓰기")
    class Rewrite {

        @Test
        @DisplayName("작성 중 상태로 되돌리고 변환 텍스트와 수정 이력을 비운다")
        void resetsToDraft() {
            Writing writing = WritingFixtures.analyzedPenWriting(WRITING_ID, PROFILE_ID, "오늘 학교에서 친구랑 놀앗다");
            given(writingRepository.findWithRevisionsById(WRITING_ID)).willReturn(Optional.of(writing));

            WritingCreateResponse response = writingService.rewrite(PROFILE_ID, WRITING_ID);

            assertThat(response.status()).isEqualTo(WritingStatus.DRAFT);
            assertThat(writing.getOriginalText()).isNull();
            assertThat(writing.getFinalText()).isNull();
            assertThat(writing.getSubmittedAt()).isNull();
            assertThat(writing.getRevisions()).isEmpty();
            // 획 데이터는 지우지 않고 시도 번호로만 구분한다.
            assertThat(writing.getAttemptNo()).isEqualTo(2);
            verify(activityEventService).record(
                    eq(ActivityEventType.WRITING_REWRITTEN), eq(PROFILE_ID), eq(WRITING_ID), any());
        }

        @Test
        @DisplayName("아직 변환이 끝나지 않은 글은 다시 쓸 수 없다")
        void rejectsBeforeAnalysis() {
            Writing writing = WritingFixtures.penWriting(WRITING_ID, PROFILE_ID);
            writing.submitHandwriting();
            given(writingRepository.findWithRevisionsById(WRITING_ID)).willReturn(Optional.of(writing));

            assertThatThrownBy(() -> writingService.rewrite(PROFILE_ID, WRITING_ID))
                    .isInstanceOf(WritingException.class)
                    .extracting(exception -> ((WritingException) exception).getErrorCode())
                    .isEqualTo(WritingErrorCode.NOT_REWRITABLE);
        }

        @Test
        @DisplayName("다른 아동의 글은 다시 쓸 수 없다")
        void rejectsOtherProfile() {
            Writing writing = WritingFixtures.analyzedPenWriting(WRITING_ID, OTHER_PROFILE_ID, "오늘 놀앗다");
            given(writingRepository.findWithRevisionsById(WRITING_ID)).willReturn(Optional.of(writing));

            assertThatThrownBy(() -> writingService.rewrite(PROFILE_ID, WRITING_ID))
                    .isInstanceOf(WritingException.class)
                    .extracting(exception -> ((WritingException) exception).getErrorCode())
                    .isEqualTo(WritingErrorCode.FORBIDDEN_PROFILE);
        }
    }
}
