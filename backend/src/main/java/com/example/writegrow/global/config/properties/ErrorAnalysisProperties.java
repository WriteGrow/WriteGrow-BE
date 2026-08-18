package com.example.writegrow.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 오류 분석 설정. (기능명세서 REQ-03)
 *
 * <p>OCR 임계값({@link OcrProperties})과 일부러 분리했다. OCR 확신도는 "글자를 잘못 읽었을
 * 가능성"이고 여기는 "교정이 필요하다고 볼 수 있는가"라, 같은 값으로 묶으면 한쪽을 조정할 때
 * 다른 쪽이 함께 움직인다.
 *
 * @param confidenceThreshold 이 값 미만의 오류 후보는 아동에게 노출하지 않고 보호자·교사
 *                            검토 대상으로만 분리하며, 반복 오류 프로필에도 반영하지 않는다.
 */
@ConfigurationProperties(prefix = "writegrow.error-analysis")
public record ErrorAnalysisProperties(
        double confidenceThreshold
) {
}
