package com.example.writegrow.domain.handwriting.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.writegrow.domain.handwriting.entity.StrokeBatch;
import com.example.writegrow.domain.handwriting.entity.StrokeData;
import com.example.writegrow.domain.handwriting.entity.StrokePayload;
import com.example.writegrow.domain.handwriting.entity.StrokePoint;
import com.example.writegrow.domain.handwriting.service.StrokeMerger.MergedStrokes;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StrokeMerger 단위 테스트")
class StrokeMergerTest {

    @Test
    @DisplayName("배치가 순서 없이 들어와도 획 순번대로 병합된다")
    void mergesInStrokeOrder() {
        StrokeBatch second = batch(1, stroke(2, 2000, 2400), stroke(3, 2500, 2900));
        StrokeBatch first = batch(0, stroke(0, 100, 500), stroke(1, 600, 1000));

        MergedStrokes merged = StrokeMerger.merge(List.of(second, first));

        assertThat(merged.strokeCount()).isEqualTo(4);
        assertThat(merged.strokes()).extracting(StrokeData::index).containsExactly(0, 1, 2, 3);
    }

    @Test
    @DisplayName("전체 소요 시간은 첫 획을 시작한 시점부터 마지막 획을 뗀 시점까지다")
    void calculatesTotalDuration() {
        MergedStrokes merged = StrokeMerger.merge(List.of(
                batch(0, stroke(0, 100, 500)),
                batch(1, stroke(1, 2500, 2900))));

        assertThat(merged.totalDurationMs()).isEqualTo(2800);
    }

    @Test
    @DisplayName("획이 하나도 없으면 소요 시간은 0 이다")
    void handlesEmptyBatches() {
        MergedStrokes merged = StrokeMerger.merge(List.of());

        assertThat(merged.strokeCount()).isZero();
        assertThat(merged.totalDurationMs()).isZero();
        assertThat(merged.strokes()).isEmpty();
    }

    private static StrokeBatch batch(int batchSeq, StrokeData... strokes) {
        // 병합은 이미 한 시도로 좁혀진 배치만 받으므로 시도 번호는 고정해 둔다.
        return StrokeBatch.of(1L, 1, batchSeq, StrokePayload.of(List.of(strokes)));
    }

    private static StrokeData stroke(int index, long penDownAt, long penUpAt) {
        return new StrokeData(index, penDownAt, penUpAt,
                List.of(new StrokePoint(10.0, 20.0, penDownAt, 0.5)));
    }
}
