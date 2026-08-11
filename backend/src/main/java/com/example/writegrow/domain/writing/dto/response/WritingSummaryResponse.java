package com.example.writegrow.domain.writing.dto.response;

import com.example.writegrow.domain.writing.entity.InputType;
import com.example.writegrow.domain.writing.entity.Writing;
import com.example.writegrow.domain.writing.entity.WritingStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "이전 글 목록 항목")
public record WritingSummaryResponse(

        @Schema(description = "글 ID", example = "1")
        Long writingId,

        @Schema(description = "입력 방식", example = "PEN")
        InputType inputType,

        @Schema(description = "글 상태", example = "CONFIRMED")
        WritingStatus status,

        @Schema(description = "글쓰기 주제", example = "오늘 있었던 일")
        String topic,

        @Schema(description = "최종 수정본 미리보기(최대 30자)", example = "오늘 학교에서 친구랑 놀았다")
        String preview,

        @Schema(description = "작성 시각", example = "2026-08-09T20:11:00")
        LocalDateTime createdAt,

        @Schema(description = "제출 시각", example = "2026-08-09T20:14:00")
        LocalDateTime submittedAt
) {

    private static final int PREVIEW_LENGTH = 30;

    public static WritingSummaryResponse from(Writing writing) {
        return new WritingSummaryResponse(
                writing.getId(),
                writing.getInputType(),
                writing.getStatus(),
                writing.getTopic(),
                preview(writing),
                writing.getCreatedAt(),
                writing.getSubmittedAt());
    }

    private static String preview(Writing writing) {
        String source = writing.getFinalText() != null ? writing.getFinalText() : writing.getOriginalText();
        if (source == null) {
            return null;
        }
        return source.length() <= PREVIEW_LENGTH ? source : source.substring(0, PREVIEW_LENGTH) + "...";
    }
}
