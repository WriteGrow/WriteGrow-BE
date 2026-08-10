package com.example.writegrow.domain.account.dto.response;

import com.example.writegrow.domain.account.entity.Profile;
import com.example.writegrow.domain.account.entity.ProfileRole;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "프로필 응답")
public record ProfileResponse(

        @Schema(description = "프로필 ID", example = "1")
        Long id,

        @Schema(description = "소속 계정 ID", example = "1")
        Long accountId,

        @Schema(description = "역할", example = "CHILD")
        ProfileRole role,

        @Schema(description = "프로필 이름", example = "민준")
        String nickname,

        @Schema(description = "출생 연도", example = "2018")
        Integer birthYear,

        @Schema(description = "보호자 동의 확인 여부", example = "true")
        boolean consentConfirmed
) {

    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(
                profile.getId(),
                profile.getAccount().getId(),
                profile.getRole(),
                profile.getNickname(),
                profile.getBirthYear(),
                profile.isConsentConfirmed());
    }
}
