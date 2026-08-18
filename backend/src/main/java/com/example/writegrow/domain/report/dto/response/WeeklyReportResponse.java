package com.example.writegrow.domain.report.dto.response;

import com.example.writegrow.domain.analysis.entity.ErrorType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * 보호자 주간 성장 리포트. (기능명세서 REQ-06 기능 1)
 *
 * <p>모든 세부 오류를 나열하지 않고 지도 우선순위를 파악할 수준으로 요약한다.
 * 낮은 확신도 후보는 집계에서 빠지며, 검토 대기 건수만 별도로 알려준다.
 */
@Schema(description = "보호자 주간 성장 리포트")
public record WeeklyReportResponse(

        @Schema(description = "아동 프로필 ID", example = "1")
        Long profileId,

        @Schema(description = "아동 이름", example = "민준")
        String nickname,

        @Schema(description = "리포트 주 시작일(월요일)", example = "2026-08-17")
        LocalDate weekStart,

        @Schema(description = "리포트 주 종료일(일요일)", example = "2026-08-23")
        LocalDate weekEnd,

        @Schema(description = "해당 주에 작성한 글이 있는지. false 면 작성 공백을 표시한다.", example = "true")
        boolean hasWriting,

        @Schema(description = "주간 요약 지표")
        Summary summary,

        @Schema(description = "주요 반복 오류. 누적 발생이 잦은 순이다.")
        List<RepeatedError> repeatedErrors,

        @Schema(description = "일자별 작성량과 오류 변화 추이")
        List<DailyTrend> dailyTrends,

        @Schema(description = "다음 집중 영역. 작성 기록이 없으면 null.")
        FocusArea nextFocus
) {

    @Schema(description = "주간 요약")
    public record Summary(

            @Schema(description = "이번 주 작성 횟수", example = "5")
            int writingCount,

            @Schema(description = "그중 최종본까지 확정된 글 수", example = "5")
            int confirmedCount,

            @Schema(description = "자기교정 횟수. 아동이 변환 텍스트를 직접 고친 횟수다.", example = "3")
            int selfCorrectionCount,

            @Schema(description = "지난 주 대비 자기교정 증감", example = "1")
            int selfCorrectionDelta,

            @Schema(description = "확정 오류 수. 낮은 확신도 후보는 제외된 값이다.", example = "4")
            long confirmedErrorCount,

            @Schema(description = "지난 주 확정 오류 수", example = "6")
            long previousWeekErrorCount,

            @Schema(description = "지난 주 대비 오류 증감. 음수면 줄었다는 뜻이다.", example = "-2")
            long errorCountDelta,

            @Schema(description = "누적 반복 오류 유형 수", example = "4")
            int repeatedErrorTypeCount,

            @Schema(description = "보호자 검토를 기다리는 낮은 확신도 후보 수", example = "2")
            long reviewPendingCount
    ) {
    }

    @Schema(description = "유형별 반복 오류")
    public record RepeatedError(

            @Schema(description = "오류 유형", example = "SPACING")
            ErrorType errorType,

            @Schema(description = "화면에 보여줄 유형 이름", example = "띄어쓰기")
            String label,

            @Schema(description = "누적 발생 횟수", example = "12")
            int cumulativeCount,

            @Schema(description = "이번 주 발생 횟수", example = "3")
            long weeklyCount,

            @Schema(description = "최근 발생일", example = "2026-08-18")
            LocalDate lastOccurredOn
    ) {
    }

    @Schema(description = "일자별 추이")
    public record DailyTrend(

            @Schema(description = "날짜", example = "2026-08-18")
            LocalDate date,

            @Schema(description = "작성한 글 수", example = "1")
            int writingCount,

            @Schema(description = "작성한 문장 수", example = "3")
            int sentenceCount,

            @Schema(description = "확정 오류 수", example = "2")
            long errorCount,

            @Schema(description = "자기교정 횟수", example = "1")
            int selfCorrectionCount
    ) {
    }

    /**
     * 다음 집중 영역. 문구가 아니라 유형과 근거 코드만 내려준다.
     * 명세가 전문적인 진단을 제공하지 않도록 요구하므로, 표현은 화면이 정한다.
     */
    @Schema(description = "다음 집중 영역")
    public record FocusArea(

            @Schema(description = "집중할 오류 유형", example = "FINAL_CONSONANT")
            ErrorType errorType,

            @Schema(description = "화면에 보여줄 유형 이름", example = "받침")
            String label,

            @Schema(description = "선정 근거", example = "MOST_REPEATED")
            FocusReason reason,

            @Schema(description = "근거가 된 값. MOST_REPEATED 면 누적 발생 횟수다.", example = "12")
            int basisValue
    ) {
    }

    @Schema(description = "집중 영역 선정 근거 (MOST_REPEATED: 가장 많이 반복됨)")
    public enum FocusReason {
        MOST_REPEATED
    }
}
