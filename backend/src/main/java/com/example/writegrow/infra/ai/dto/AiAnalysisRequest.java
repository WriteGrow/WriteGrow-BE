package com.example.writegrow.infra.ai.dto;

/**
 * 백엔드 → AI 서버 분석 요청.
 *
 * <p>이미지와 획 데이터는 백엔드가 S3 에 올린 뒤 임시 열람 URL 로만 전달한다.
 * AI 서버는 결과 이미지와 획의 시계열을 함께 보고 OCR 과 과정 분석을 수행한다.
 */
public record AiAnalysisRequest(
        Long writingId,
        String imageUrl,
        String strokeUrl,
        Canvas canvas,
        String expectedTopic
) {

    public record Canvas(Integer width, Integer height) {
    }
}
