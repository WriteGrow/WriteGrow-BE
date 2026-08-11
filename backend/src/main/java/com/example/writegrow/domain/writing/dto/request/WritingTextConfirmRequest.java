package com.example.writegrow.domain.writing.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "OCR 변환 텍스트 확인/수정 후 최종본 확정 요청")
public record WritingTextConfirmRequest(

        @Schema(description = "아동이 확인하거나 고친 최종 텍스트", example = "오늘 학교에서 친구랑 놀았다")
        @NotBlank(message = "글 내용은 비어 있을 수 없습니다.")
        String content
) {
}
