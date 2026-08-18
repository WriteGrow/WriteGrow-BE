package com.example.writegrow.domain.report.dto.response;

import com.example.writegrow.domain.analysis.entity.ErrorType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 보호자 홈. 연결된 자녀별 이번 주 현황 카드를 담는다.
 */
@Schema(description = "보호자 홈 — 자녀별 이번 주 글쓰기 현황")
public record ParentHomeResponse(

        @Schema(description = "계정 ID", example = "1")
        Long accountId,

        @Schema(description = "연결된 자녀 카드. 등록 순으로 고정된다.")
        List<ChildCard> children
) {

    @Schema(description = "자녀 카드")
    public record ChildCard(

            @Schema(description = "아동 프로필 ID", example = "1")
            Long profileId,

            @Schema(description = "이름", example = "민준")
            String nickname,

            @Schema(description = "나이(만). 출생 연도가 없으면 null.", example = "8")
            Integer age,

            @Schema(description = "이번 주 작성 편수", example = "3")
            int weeklyWritingCount,

            @Schema(description = "이번 주 자기교정 횟수", example = "2")
            int selfCorrectionCount,

            @Schema(description = "연속 작성 일수", example = "5")
            int writingStreakDays,

            @Schema(description = "최근 글 ID. 글이 없으면 null.", example = "12")
            Long recentWritingId,

            @Schema(description = "최근 글 미리보기. 글이 없으면 null.", example = "오늘 강아지랑 산책했어요")
            String recentWritingPreview,

            @Schema(description = "주요 반복 오류 유형. 누적이 잦은 순으로 최대 2개.")
            List<ErrorType> topErrorTypes,

            @Schema(description = "이번 주 확정 오류 수", example = "4")
            long weeklyErrorCount,

            @Schema(description = "지난 주 대비 증감. 음수면 줄었다는 뜻이다.", example = "-2")
            long errorCountDelta
    ) {
    }
}
