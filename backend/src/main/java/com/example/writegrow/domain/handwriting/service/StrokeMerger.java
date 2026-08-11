package com.example.writegrow.domain.handwriting.service;

import com.example.writegrow.domain.handwriting.entity.StrokeBatch;
import com.example.writegrow.domain.handwriting.entity.StrokeData;
import java.util.Comparator;
import java.util.List;

/**
 * 작성 중 나눠 받은 획 배치를 하나의 시계열로 병합한다.
 *
 * <p>이 데이터가 "결과가 아니라 과정을 본다"는 서비스 차별점의 원천이므로, 순서와 시각 정보를 잃지 않도록 병합한다.
 */
public final class StrokeMerger {

    private StrokeMerger() {
    }

    /**
     * @param strokes         배치 순번 → 획 순번으로 정렬된 전체 획
     * @param totalDurationMs 첫 획을 시작한 시점부터 마지막 획을 뗀 시점까지의 시간
     */
    public record MergedStrokes(List<StrokeData> strokes, int strokeCount, long totalDurationMs) {
    }

    public static MergedStrokes merge(List<StrokeBatch> batches) {
        List<StrokeData> strokes = batches.stream()
                .sorted(Comparator.comparingInt(StrokeBatch::getBatchSeq))
                .map(StrokeBatch::getPayload)
                .filter(payload -> payload != null && payload.strokes() != null)
                .flatMap(payload -> payload.strokes().stream())
                .sorted(Comparator.comparingInt(StrokeData::index))
                .toList();

        return new MergedStrokes(strokes, strokes.size(), totalDurationMs(strokes));
    }

    private static long totalDurationMs(List<StrokeData> strokes) {
        if (strokes.isEmpty()) {
            return 0L;
        }
        long firstPenDown = strokes.stream().mapToLong(StrokeData::penDownAt).min().orElse(0L);
        long lastPenUp = strokes.stream().mapToLong(StrokeData::penUpAt).max().orElse(0L);
        return Math.max(0L, lastPenUp - firstPenDown);
    }
}
