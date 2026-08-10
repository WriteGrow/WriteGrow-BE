package com.example.writegrow.global.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 손글씨 분석을 담당하는 AI 서버(WriteGrow-BE/ai) 연동 설정.
 *
 * @param stub true 면 실제 호출 대신 {@code StubAiAnalysisClient} 가 고정 응답을 돌려준다.
 *             AI 서버 없이 전체 흐름을 검증할 때 사용한다.
 */
@ConfigurationProperties(prefix = "writegrow.ai")
public record AiProperties(
        String baseUrl,
        Duration connectTimeout,
        Duration readTimeout,
        boolean stub
) {
}
