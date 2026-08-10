package com.example.writegrow.domain.account.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "가족 계정 생성 요청")
public record AccountCreateRequest(

        @Schema(description = "계정 이름", example = "김민준네 가족")
        @NotBlank(message = "계정 이름은 비어 있을 수 없습니다.")
        @Size(max = 50, message = "계정 이름은 50자를 넘을 수 없습니다.")
        String name
) {
}
