package com.example.writegrow.domain.account.dto.response;

import com.example.writegrow.domain.account.entity.Account;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;

@Schema(description = "가족 계정 응답")
public record AccountResponse(

        @Schema(description = "계정 ID", example = "1")
        Long id,

        @Schema(description = "계정 이름", example = "김민준네 가족")
        String name,

        @Schema(description = "생성 시각", example = "2026-08-09T21:30:00")
        LocalDateTime createdAt
) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getId(), account.getName(), account.getCreatedAt());
    }
}
