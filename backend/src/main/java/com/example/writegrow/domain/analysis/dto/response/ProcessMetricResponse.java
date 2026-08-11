package com.example.writegrow.domain.analysis.dto.response;

import com.example.writegrow.domain.analysis.entity.HesitationPoint;
import com.example.writegrow.domain.analysis.entity.WritingProcessMetric;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "글을 쓰는 과정에 대한 분석 지표")
public record ProcessMetricResponse(

        @Schema(description = "글을 쓰는 데 걸린 전체 시간(ms)", example = "92000")
        Long totalDurationMs,

        @Schema(description = "멈춘 횟수", example = "4")
        Integer pauseCount,

        @Schema(description = "가장 오래 멈춘 시간(ms)", example = "7300")
        Long longestPauseMs,

        @Schema(description = "획 하나당 평균 소요 시간(ms)", example = "410")
        Long avgStrokeDurationMs,

        @Schema(description = "머뭇거린 글자와 자모 목록")
        List<HesitationPoint> hesitationPoints
) {

    public static ProcessMetricResponse from(WritingProcessMetric metric) {
        return new ProcessMetricResponse(
                metric.getTotalDurationMs(),
                metric.getPauseCount(),
                metric.getLongestPauseMs(),
                metric.getAvgStrokeDurationMs(),
                metric.getHesitationPoints() == null ? List.of() : metric.getHesitationPoints());
    }
}
