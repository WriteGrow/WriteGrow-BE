package com.example.writegrow.domain.handwriting.dto.response;

import com.example.writegrow.domain.handwriting.entity.StrokeData;
import java.util.List;

/**
 * S3 에 저장되는 병합 획 문서. AI 서버가 이 JSON 을 그대로 내려받아 과정 분석에 사용한다.
 */
public record MergedStrokeDocument(
        Long writingId,
        Integer canvasWidth,
        Integer canvasHeight,
        int strokeCount,
        long totalDurationMs,
        List<StrokeData> strokes
) {
}
