package com.example.writegrow.domain.handwriting.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 펜을 대고 뗄 때까지의 획 하나.
 */
@Schema(description = "획 한 개")
public record StrokeData(

        @Schema(description = "글 전체에서의 획 순번(0부터)", example = "0")
        int index,

        @Schema(description = "펜을 댄 시각(ms, 세션 시작 기준)", example = "1200")
        long penDownAt,

        @Schema(description = "펜을 뗀 시각(ms, 세션 시작 기준)", example = "1620")
        long penUpAt,

        @Schema(description = "획을 이루는 점 목록")
        List<StrokePoint> points
) {

    public long durationMs() {
        return Math.max(0, penUpAt - penDownAt);
    }
}
