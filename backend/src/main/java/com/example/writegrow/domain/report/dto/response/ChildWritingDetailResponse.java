package com.example.writegrow.domain.report.dto.response;

import com.example.writegrow.domain.analysis.dto.response.ErrorCandidateResponse;
import com.example.writegrow.domain.writing.dto.response.WritingRevisionResponse;
import com.example.writegrow.domain.writing.entity.InputType;
import com.example.writegrow.domain.writing.entity.WritingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 보호자가 여는 개별 글의 수정 전후 열람. (기능명세서 REQ-06 기능 1)
 *
 * <p>아동용 상세 조회와 응답을 나눈 것은 담는 내용이 다르기 때문이다. 여기에는 교정 유형과
 * 검토 대기 건수처럼 보호자만 보는 값이 들어간다.
 */
@Schema(description = "보호자용 글 수정 전후 열람")
public record ChildWritingDetailResponse(

        @Schema(description = "글 ID", example = "1")
        Long writingId,

        @Schema(description = "아동 프로필 ID", example = "1")
        Long profileId,

        @Schema(description = "아동 이름", example = "민준")
        String nickname,

        @Schema(description = "글쓰기 주제", example = "오늘 있었던 일")
        String topic,

        @Schema(description = "입력 방식", example = "KEYBOARD")
        InputType inputType,

        @Schema(description = "글 상태", example = "CONFIRMED")
        WritingStatus status,

        @Schema(description = "작성 시각", example = "2026-08-18T15:48:00")
        LocalDateTime createdAt,

        @Schema(description = "제출 시각", example = "2026-08-18T15:52:00")
        LocalDateTime submittedAt,

        @Schema(description = "수정 전 원문", example = "오늘 학교에서 친구랑 놀앗다")
        String originalText,

        @Schema(description = "최종 수정본", example = "오늘 학교에서 친구랑 놀았다")
        String finalText,

        @Schema(description = "문장 수", example = "1")
        int sentenceCount,

        @Schema(description = "아동이 직접 고친 횟수", example = "1")
        int selfCorrectionCount,

        @Schema(description = "수정 이력. 수정 전/후 비교의 근거다.")
        List<WritingRevisionResponse> revisions,

        @Schema(description = "확정 오류. 교정 유형을 여기서 읽는다.")
        List<ErrorCandidateResponse> confirmedErrors,

        @Schema(description = "검토를 기다리는 낮은 확신도 후보 수", example = "1")
        int reviewPendingCount
) {
}
