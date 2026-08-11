package com.example.writegrow.domain.writing.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.writegrow.domain.writing.exception.WritingErrorCode;
import com.example.writegrow.domain.writing.exception.WritingException;
import com.example.writegrow.support.WritingFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Writing 엔티티 상태 전이 테스트")
class WritingTest {

    @Nested
    @DisplayName("키보드 글 제출")
    class SubmitKeyboard {

        @Test
        @DisplayName("제출하면 원문과 최종본이 같은 값으로 확정되고 INITIAL 이력이 남는다")
        void confirmsImmediately() {
            Writing writing = WritingFixtures.keyboardWriting(1L, 1L);

            writing.submitKeyboard("오늘 학교에서 친구랑 놀았다");

            assertThat(writing.getStatus()).isEqualTo(WritingStatus.CONFIRMED);
            assertThat(writing.getOriginalText()).isEqualTo("오늘 학교에서 친구랑 놀았다");
            assertThat(writing.getFinalText()).isEqualTo("오늘 학교에서 친구랑 놀았다");
            assertThat(writing.getSubmittedAt()).isNotNull();
            assertThat(writing.getRevisions()).hasSize(1);
            assertThat(writing.getRevisions().getFirst().getSource()).isEqualTo(RevisionSource.INITIAL);
            assertThat(writing.getRevisions().getFirst().getRevisionNo()).isEqualTo(1);
        }

        @ParameterizedTest(name = "내용이 [{0}] 이면 EMPTY_CONTENT")
        @ValueSource(strings = {"", " ", "\n", "\t  "})
        @DisplayName("빈 글은 제출할 수 없다")
        void rejectsBlankContent(String content) {
            Writing writing = WritingFixtures.keyboardWriting(1L, 1L);

            assertThatThrownBy(() -> writing.submitKeyboard(content))
                    .isInstanceOf(WritingException.class)
                    .extracting(exception -> ((WritingException) exception).getErrorCode())
                    .isEqualTo(WritingErrorCode.EMPTY_CONTENT);
        }

        @Test
        @DisplayName("null 내용도 제출할 수 없다")
        void rejectsNullContent() {
            Writing writing = WritingFixtures.keyboardWriting(1L, 1L);

            assertThatThrownBy(() -> writing.submitKeyboard(null))
                    .isInstanceOf(WritingException.class)
                    .extracting(exception -> ((WritingException) exception).getErrorCode())
                    .isEqualTo(WritingErrorCode.EMPTY_CONTENT);
        }

        @Test
        @DisplayName("이미 제출한 글은 다시 제출할 수 없다")
        void rejectsResubmit() {
            Writing writing = WritingFixtures.keyboardWriting(1L, 1L);
            writing.submitKeyboard("첫 제출");

            assertThatThrownBy(() -> writing.submitKeyboard("두 번째 제출"))
                    .isInstanceOf(WritingException.class)
                    .extracting(exception -> ((WritingException) exception).getErrorCode())
                    .isEqualTo(WritingErrorCode.ALREADY_SUBMITTED);
        }

        @Test
        @DisplayName("손글씨 글에 키보드 제출을 호출하면 INVALID_INPUT_TYPE")
        void rejectsWrongInputType() {
            Writing writing = WritingFixtures.penWriting(1L, 1L);

            assertThatThrownBy(() -> writing.submitKeyboard("손글씨인데 키보드 제출"))
                    .isInstanceOf(WritingException.class)
                    .extracting(exception -> ((WritingException) exception).getErrorCode())
                    .isEqualTo(WritingErrorCode.INVALID_INPUT_TYPE);
        }
    }

    @Nested
    @DisplayName("손글씨 글 흐름")
    class Handwriting {

        @Test
        @DisplayName("제출하면 분석 대기 상태가 되고 최종본은 아직 비어 있다")
        void submitMovesToSubmitted() {
            Writing writing = WritingFixtures.penWriting(1L, 1L);

            writing.submitHandwriting();

            assertThat(writing.getStatus()).isEqualTo(WritingStatus.SUBMITTED);
            assertThat(writing.getSubmittedAt()).isNotNull();
            assertThat(writing.getFinalText()).isNull();
            assertThat(writing.getRevisions()).isEmpty();
        }

        @Test
        @DisplayName("OCR 결과를 반영하면 원문이 채워지고 OCR 이력이 남는다")
        void applyOcrText() {
            Writing writing = WritingFixtures.penWriting(1L, 1L);
            writing.submitHandwriting();

            writing.applyOcrText("오늘 학교에서 친구랑 놀앗다");

            assertThat(writing.getStatus()).isEqualTo(WritingStatus.ANALYZED);
            assertThat(writing.getOriginalText()).isEqualTo("오늘 학교에서 친구랑 놀앗다");
            assertThat(writing.getFinalText()).isNull();
            assertThat(writing.getRevisions()).hasSize(1);
            assertThat(writing.getRevisions().getFirst().getSource()).isEqualTo(RevisionSource.OCR);
        }

        @Test
        @DisplayName("분석 실패 후 재시도하면 다시 분석 대기 상태가 된다")
        void retryAfterFailure() {
            Writing writing = WritingFixtures.penWriting(1L, 1L);
            writing.submitHandwriting();
            writing.markAnalysisFailed();

            assertThat(writing.getStatus()).isEqualTo(WritingStatus.ANALYSIS_FAILED);

            writing.markAnalysisRetried();
            assertThat(writing.getStatus()).isEqualTo(WritingStatus.SUBMITTED);
        }
    }

    @Nested
    @DisplayName("최종본 확정")
    class ConfirmText {

        @Test
        @DisplayName("아동이 텍스트를 고치면 CHILD_EDIT 이력이 추가되고 edited 가 true 다")
        void recordsChildEdit() {
            Writing writing = WritingFixtures.analyzedPenWriting(1L, 1L, "오늘 학교에서 친구랑 놀앗다");

            boolean edited = writing.confirmText("오늘 학교에서 친구랑 놀았다");

            assertThat(edited).isTrue();
            assertThat(writing.getStatus()).isEqualTo(WritingStatus.CONFIRMED);
            assertThat(writing.getFinalText()).isEqualTo("오늘 학교에서 친구랑 놀았다");
            assertThat(writing.getRevisions()).hasSize(2);
            assertThat(writing.getRevisions().get(1).getSource()).isEqualTo(RevisionSource.CHILD_EDIT);
            assertThat(writing.getRevisions().get(1).getRevisionNo()).isEqualTo(2);
        }

        @Test
        @DisplayName("변환 결과 그대로 확정하면 수정 이력이 늘지 않는다")
        void withoutEdit() {
            Writing writing = WritingFixtures.analyzedPenWriting(1L, 1L, "오늘 학교에서 친구랑 놀았다");

            boolean edited = writing.confirmText("오늘 학교에서 친구랑 놀았다");

            assertThat(edited).isFalse();
            assertThat(writing.getRevisions()).hasSize(1);
        }

        @Test
        @DisplayName("분석이 끝나기 전에는 확정할 수 없다")
        void rejectsBeforeAnalysis() {
            Writing writing = WritingFixtures.penWriting(1L, 1L);
            writing.submitHandwriting();

            assertThatThrownBy(() -> writing.confirmText("아직 분석 안 끝났음"))
                    .isInstanceOf(WritingException.class)
                    .extracting(exception -> ((WritingException) exception).getErrorCode())
                    .isEqualTo(WritingErrorCode.NOT_READY_FOR_CONFIRM);
        }

        @Test
        @DisplayName("이미 확정된 글은 다시 확정할 수 없다")
        void rejectsAlreadyConfirmed() {
            Writing writing = WritingFixtures.analyzedPenWriting(1L, 1L, "오늘 학교에서 친구랑 놀았다");
            writing.confirmText("오늘 학교에서 친구랑 놀았다");

            assertThatThrownBy(() -> writing.confirmText("다시 확정"))
                    .isInstanceOf(WritingException.class)
                    .extracting(exception -> ((WritingException) exception).getErrorCode())
                    .isEqualTo(WritingErrorCode.ALREADY_CONFIRMED);
        }
    }

    @Nested
    @DisplayName("소유권 검증")
    class ValidateOwner {

        @Test
        @DisplayName("작성자 본인이면 통과한다")
        void allowsOwner() {
            Writing writing = WritingFixtures.keyboardWriting(1L, 7L);

            writing.validateOwner(7L);
        }

        @Test
        @DisplayName("다른 프로필이면 FORBIDDEN_PROFILE 예외가 발생한다")
        void rejectsOthers() {
            Writing writing = WritingFixtures.keyboardWriting(1L, 7L);

            assertThatThrownBy(() -> writing.validateOwner(8L))
                    .isInstanceOf(WritingException.class)
                    .extracting(exception -> ((WritingException) exception).getErrorCode())
                    .isEqualTo(WritingErrorCode.FORBIDDEN_PROFILE);
        }
    }
}
