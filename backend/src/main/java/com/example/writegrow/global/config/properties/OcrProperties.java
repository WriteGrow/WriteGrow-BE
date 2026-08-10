package com.example.writegrow.global.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param confidenceThreshold 이 값 미만의 확신도를 가진 구절은 아동 교정 대상에서 제외하고
 *                            보호자/교사 검토 대상으로만 분리한다. (기능명세서 FEAT-02-01)
 */
@ConfigurationProperties(prefix = "writegrow.ocr")
public record OcrProperties(
        double confidenceThreshold
) {
}
