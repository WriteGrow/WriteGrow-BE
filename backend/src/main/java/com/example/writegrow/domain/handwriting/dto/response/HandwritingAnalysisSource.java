package com.example.writegrow.domain.handwriting.dto.response;

/**
 * AI 분석 서버에 넘길 원본 자료. 백엔드가 S3 에 올린 뒤 임시 열람 URL 만 전달한다.
 *
 * <p>외부로 노출되는 응답이 아니라 도메인 간 전달용 데이터다.
 */
public record HandwritingAnalysisSource(
        Long writingId,
        String imageUrl,
        String strokeDataUrl,
        Integer canvasWidth,
        Integer canvasHeight,
        int strokeCount,
        long totalDurationMs
) {
}
