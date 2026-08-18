package com.example.writegrow.infra.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/**
 * AI 서버 → 백엔드 오류 분석 응답.
 *
 * <p>AI 팀이 필드를 추가해도 백엔드가 깨지지 않도록 모르는 필드는 무시한다.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AiErrorAnalysisResponse(
        List<ErrorItem> errors
) {

    /**
     * @param type       오류 유형. 문자열로 받아 백엔드에서 변환한다. enum 으로 직접 역직렬화하면
     *                   AI 가 새 유형을 하나 추가했을 때 분석 응답 전체가 깨진다.
     * @param confidence 0.0 ~ 1.0. 운영 기준 미만이면 아동에게 노출하지 않고 검토 대상으로만 분리한다.
     * @param reason     판단 근거. 보호자가 낮은 확신도 후보를 검토할 때의 재료다.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ErrorItem(
            String type,
            Integer startIndex,
            Integer endIndex,
            String original,
            String suggestion,
            Double confidence,
            String reason
    ) {
    }
}
