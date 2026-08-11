package com.example.writegrow.domain.writing.dto.response;

import com.example.writegrow.domain.writing.entity.InputType;
import com.example.writegrow.domain.writing.entity.Writing;
import com.example.writegrow.domain.writing.entity.WritingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "글쓰기 시작 응답")
public record WritingCreateResponse(

        @Schema(description = "글 ID", example = "1")
        Long writingId,

        @Schema(description = "입력 방식", example = "PEN")
        InputType inputType,

        @Schema(description = "글 상태", example = "DRAFT")
        WritingStatus status
) {

    public static WritingCreateResponse from(Writing writing) {
        return new WritingCreateResponse(writing.getId(), writing.getInputType(), writing.getStatus());
    }
}
