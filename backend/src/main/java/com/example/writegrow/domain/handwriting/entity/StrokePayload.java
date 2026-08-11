package com.example.writegrow.domain.handwriting.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 한 번에 전송된 획 묶음. JSONB 컬럼에 그대로 직렬화된다.
 */
@Schema(description = "획 묶음")
public record StrokePayload(

        @Schema(description = "획 목록")
        List<StrokeData> strokes
) {

    public static StrokePayload of(List<StrokeData> strokes) {
        return new StrokePayload(strokes == null ? List.of() : strokes);
    }

    public int strokeCount() {
        return strokes == null ? 0 : strokes.size();
    }
}
