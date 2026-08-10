package com.example.writegrow.domain.account.dto.request;

import com.example.writegrow.domain.account.entity.ProfileRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "프로필 생성 요청")
public record ProfileCreateRequest(

        @Schema(description = "역할", example = "CHILD")
        @NotNull(message = "역할은 필수입니다.")
        ProfileRole role,

        @Schema(description = "프로필 이름", example = "민준")
        @NotBlank(message = "이름은 비어 있을 수 없습니다.")
        @Size(max = 30, message = "이름은 30자를 넘을 수 없습니다.")
        String nickname,

        @Schema(description = "출생 연도", example = "2018")
        @Min(value = 1900, message = "출생 연도가 올바르지 않습니다.")
        @Max(value = 2100, message = "출생 연도가 올바르지 않습니다.")
        Integer birthYear
) {
}
