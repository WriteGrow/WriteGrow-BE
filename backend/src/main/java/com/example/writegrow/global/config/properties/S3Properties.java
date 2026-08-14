package com.example.writegrow.global.config.properties;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param endpoint        비어 있으면 실제 AWS 엔드포인트를 사용한다. S3 호환 스토리지를 쓸 때만 지정한다.
 * @param keyPrefix       모든 객체 키 앞에 붙는 환경 구분자(`dev/`, `prod/`). dev 와 prod 가 같은 버킷을
 *                        나눠 쓰므로, 이 값이 겹치면 로컬에서 운영 데이터를 건드리게 된다.
 * @param verifyOnStartup true 면 기동 시 {@code S3BucketVerifier} 가 버킷 접근을 확인하고,
 *                        실패하면 애플리케이션을 띄우지 않는다. 실제 버킷이 없는 환경에서는 false 로 둔다.
 */
@ConfigurationProperties(prefix = "writegrow.s3")
public record S3Properties(
        String bucket,
        String region,
        String endpoint,
        String keyPrefix,
        String accessKey,
        String secretKey,
        Duration presignDuration,
        boolean verifyOnStartup
) {

    public boolean hasCustomEndpoint() {
        return endpoint != null && !endpoint.isBlank();
    }

    /**
     * 항상 {@code /} 로 끝나는 형태로 돌려준다. 설정에서 슬래시를 빠뜨려도
     * {@code devhandwriting/...} 처럼 붙어버리지 않게 한다.
     */
    public String normalizedKeyPrefix() {
        if (keyPrefix == null || keyPrefix.isBlank()) {
            return "";
        }
        return keyPrefix.endsWith("/") ? keyPrefix : keyPrefix + "/";
    }

    /**
     * path-style 접근은 커스텀 엔드포인트(LocalStack·MinIO)에서만 필요하다. 실제 AWS 는
     * virtual-hosted 방식이 기본이다.
     *
     * <p>설정으로 따로 두지 않고 엔드포인트에서 유도한다. 두 값은 항상 함께 뒤집혀야 하는데
     * 따로 두면 하나만 바꿔 놓고 요청이 엉뚱한 곳으로 가는 사고가 난다.
     */
    public boolean pathStyleAccess() {
        return hasCustomEndpoint();
    }

    public boolean hasStaticCredentials() {
        return accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();
    }
}
