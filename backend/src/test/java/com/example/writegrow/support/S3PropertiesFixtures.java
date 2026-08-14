package com.example.writegrow.support;

import com.example.writegrow.global.config.properties.S3Properties;
import java.time.Duration;

/**
 * S3 설정 픽스처. 생성자 인자가 많아 테스트마다 나열하면 무엇을 검증하는지 묻히므로,
 * 관심 있는 값만 지정하고 나머지는 여기서 채운다.
 */
public final class S3PropertiesFixtures {

    public static final String BUCKET = "writegrow-test";
    public static final String REGION = "ap-northeast-2";

    private S3PropertiesFixtures() {
    }

    public static S3Properties withKeyPrefix(String keyPrefix) {
        return of(BUCKET, "", keyPrefix);
    }

    public static S3Properties withEndpoint(String endpoint) {
        return of(BUCKET, endpoint, "test/");
    }

    public static S3Properties of(String bucket, String endpoint, String keyPrefix) {
        return new S3Properties(
                bucket, REGION, endpoint, keyPrefix, "", "", Duration.ofMinutes(10), true);
    }
}
