package com.example.writegrow.domain.writing.dto.request;

import com.example.writegrow.domain.writing.entity.InputType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "글쓰기 시작 요청")
public record WritingCreateRequest(

        @Schema(description = "입력 방식", example = "PEN")
        @NotNull(message = "입력 방식은 필수입니다.")
        InputType inputType,

        @Schema(description = "글쓰기 주제 (선택)", example = "오늘 있었던 일")
        @Size(max = 100, message = "주제는 100자를 넘을 수 없습니다.")
        String topic
) {
}
