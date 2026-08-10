package com.example.writegrow.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * AI 서버 → 백엔드 분석 응답.
 *
 * <p>AI 팀이 필드를 추가해도 백엔드가 깨지지 않도록 모르는 필드는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiAnalysisResponse(
        String fullText,
        Double overallConfidence,
        List<Segment> segments,
        ProcessMetric processMetric
) {

    /**
     * @param confidence 0.0 ~ 1.0. 운영 임계값 미만이면 아동 교정 대상에서 제외된다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Segment(
            String text,
            Double confidence,
            Integer startIndex,
            Integer endIndex
    ) {
    }

    /**
     * 결과가 아닌 "과정"에 대한 지표. 서비스 차별점의 핵심 데이터다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ProcessMetric(
            Long totalDurationMs,
            Integer pauseCount,
            Long longestPauseMs,
            Long avgStrokeDurationMs,
            List<HesitationPoint> hesitationPoints
    ) {
    }

    /**
     * 아동이 특정 글자/자모에서 머뭇거린 지점.
     *
     * @param character AI 응답의 {@code char} 필드. 자바 예약어라 이름을 바꿔 매핑한다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record HesitationPoint(
            Integer charIndex,
            @JsonProperty("char") String character,
            String jamo,
            Long durationMs,
            Integer retryCount
    ) {
    }
}
