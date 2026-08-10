package com.example.writegrow.global.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param endpoint        비어 있으면 실제 AWS 엔드포인트를 사용한다. LocalStack 사용 시에만 지정한다.
 * @param pathStyleAccess LocalStack 은 path-style 접근이 필요하다.
 */
@ConfigurationProperties(prefix = "writegrow.s3")
public record S3Properties(
        String bucket,
        String region,
        String endpoint,
        boolean pathStyleAccess,
        String accessKey,
        String secretKey,
        Duration presignDuration
) {

    public boolean hasCustomEndpoint() {
        return endpoint != null && !endpoint.isBlank();
    }

    public boolean hasStaticCredentials() {
        return accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }
}
