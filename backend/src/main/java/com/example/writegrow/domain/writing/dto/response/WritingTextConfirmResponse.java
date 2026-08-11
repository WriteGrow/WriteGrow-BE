package com.example.writegrow.domain.writing.dto.response;

import com.example.writegrow.domain.writing.entity.Writing;
import com.example.writegrow.domain.writing.entity.WritingStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "최종본 확정 응답")
public record WritingTextConfirmResponse(

        @Schema(description = "글 ID", example = "1")
        Long writingId,

        @Schema(description = "글 상태", example = "CONFIRMED")
        WritingStatus status,

        @Schema(description = "확정된 최종본", example = "오늘 학교에서 친구랑 놀았다")
        String finalText,

        @Schema(description = "아동이 변환 텍스트를 실제로 고쳤는지 여부", example = "true")
        boolean edited
) {

    public static WritingTextConfirmResponse of(Writing writing, boolean edited) {
        return new WritingTextConfirmResponse(
                writing.getId(), writing.getStatus(), writing.getFinalText(), edited);
    }
}
