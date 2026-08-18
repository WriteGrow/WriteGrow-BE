package com.example.writegrow.domain.writing.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;

/**
 * 아동 홈의 "오늘 작성 0 / 1편".
 */
@Schema(description = "오늘 작성 현황")
public record TodayWritingStatusResponse(

        @Schema(description = "아동 프로필 ID", example = "1")
        Long profileId,

        @Schema(description = "기준 날짜", example = "2026-08-18")
        LocalDate date,

        @Schema(description = "오늘 쓴 글 수", example = "0")
        int writingCount,

        @Schema(description = "하루 목표 편수", example = "1")
        int dailyGoal,

        @Schema(description = "목표 달성 여부", example = "false")
        boolean goalAchieved,

        @Schema(description = """
                작성 중인 글 ID. 있으면 "새 글쓰기" 대신 이어쓰기로 안내한다.
                없으면 null.
                """, example = "12")
        Long inProgressWritingId
) {
}
