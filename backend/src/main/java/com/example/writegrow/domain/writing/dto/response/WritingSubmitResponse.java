package com.example.writegrow.domain.writing.dto.response;

import com.example.writegrow.domain.writing.entity.Writing;
import com.example.writegrow.domain.writing.entity.WritingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "글 제출 응답")
public record WritingSubmitResponse(

        @Schema(description = "글 ID", example = "1")
        Long writingId,

        @Schema(description = "글 상태. 키보드 글은 즉시 CONFIRMED, 손글씨 글은 분석 중을 뜻하는 SUBMITTED 가 된다.",
                example = "SUBMITTED")
        WritingStatus status,

        @Schema(description = "손글씨 분석이 진행 중인지 여부", example = "true")
        boolean analysisInProgress
) {

    public static WritingSubmitResponse from(Writing writing) {
        return new WritingSubmitResponse(
                writing.getId(),
                writing.getStatus(),
                writing.getStatus() == WritingStatus.SUBMITTED);
    }
}
