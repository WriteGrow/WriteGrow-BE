package com.example.writegrow.domain.writing.dto.response;

import com.example.writegrow.domain.writing.entity.RevisionSource;
import com.example.writegrow.domain.writing.entity.WritingRevision;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "수정 이력 항목")
public record WritingRevisionResponse(

        @Schema(description = "이력 순번", example = "1")
        int revisionNo,

        @Schema(description = "해당 시점의 내용", example = "오늘 학교에서 친구랑 놀았다")
        String content,

        @Schema(description = "이력 출처", example = "OCR")
        RevisionSource source,

        @Schema(description = "기록 시각", example = "2026-08-09T20:15:00")
        LocalDateTime createdAt
) {

    public static WritingRevisionResponse from(WritingRevision revision) {
        return new WritingRevisionResponse(
                revision.getRevisionNo(),
                revision.getContent(),
                revision.getSource(),
                revision.getCreatedAt());
    }
}
