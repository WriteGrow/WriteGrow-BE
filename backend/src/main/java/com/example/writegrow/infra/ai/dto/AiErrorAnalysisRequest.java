package com.example.writegrow.infra.ai.dto;

/**
 * 백엔드 → AI 서버 오류 분석 요청.
 *
 * <p>손글씨 분석({@link AiAnalysisRequest})과 엔드포인트를 나눈 이유는 입력이 다르기 때문이다.
 * 손글씨는 이미지와 획 데이터를 보지만, 오류 분석은 확정된 텍스트만 본다. 키보드 글에는
 * 이미지가 아예 없어 같은 엔드포인트에 태울 수 없다.
 *
 * @param text  분석 대상. 아동이 확인·수정을 마친 최종 텍스트다.
 * @param topic 아동이 고른 글쓰기 주제. 문맥 판단에 참고한다. 없으면 null.
 */
public record AiErrorAnalysisRequest(
        Long writingId,
        String text,
        String topic
) {
}
