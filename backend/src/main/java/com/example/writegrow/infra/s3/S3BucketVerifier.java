package com.example.writegrow.infra.s3;

import com.example.writegrow.global.config.properties.S3Properties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * 기동 시 버킷에 실제로 접근할 수 있는지 한 번 확인한다.
 *
 * <p>확인하지 않으면 버킷명 오타나 잘못된 자격 증명이 <b>아동이 손글씨를 제출하는 순간</b>에야 드러난다.
 * 그때는 이미 획을 다 모은 뒤라 분석 실패로만 기록되고, 원인도 로그를 파야 알 수 있다.
 * 설정이 틀렸으면 트래픽을 받기 전에 죽는 편이 낫다.
 *
 * <p>{@code writegrow.s3.verify-on-startup=false} 로 끌 수 있다. 실제 버킷이 없는 환경
 * (통합 테스트 등)에서 필요하다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "writegrow.s3", name = "verify-on-startup",
        havingValue = "true", matchIfMissing = true)
public class S3BucketVerifier {

    private static final int NOT_FOUND = 404;

    private final S3Client s3Client;
    private final AwsCredentialsProvider credentialsProvider;
    private final S3Properties s3Properties;

    @PostConstruct
    void verifyBucketAccessible() {
        resolveCredentials();

        String bucket = s3Properties.bucket();
        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            log.info("S3 버킷 확인 완료: bucket={}, region={}", bucket, s3Properties.region());
        } catch (NoSuchBucketException exception) {
            throw bucketNotFound(bucket, exception);
        } catch (S3Exception exception) {
            // HeadBucket 은 응답 본문이 없어 SDK 가 오류 코드를 해석하지 못한다. 버킷이 없어도
            // NoSuchBucketException 이 아니라 404 를 담은 일반 S3Exception 으로 오는 경우가 있다.
            if (exception.statusCode() == NOT_FOUND) {
                throw bucketNotFound(bucket, exception);
            }
            throw new IllegalStateException(
                    ("S3 버킷에 접근할 수 없습니다: bucket=%s (HTTP %d). "
                            + "자격 증명과 IAM 권한(s3:ListBucket)을 확인하세요.")
                            .formatted(bucket, exception.statusCode()), exception);
        } catch (SdkException exception) {
            // 엔드포인트 오타, 네트워크 차단 등 응답 자체를 받지 못한 경우.
            throw new IllegalStateException(
                    "S3 에 연결하지 못했습니다: bucket=%s, endpoint=%s."
                            .formatted(bucket, s3Properties.hasCustomEndpoint()
                                    ? s3Properties.endpoint()
                                    : "AWS 기본"), exception);
        }
    }

    /**
     * 자격 증명을 먼저 확인한다. 이걸 하지 않으면 환경 변수를 빠뜨린 흔한 실수가 아래 HeadBucket 의
     * "연결하지 못했습니다" 로 나가서, 네트워크 문제처럼 읽힌다.
     */
    private void resolveCredentials() {
        try {
            credentialsProvider.resolveCredentials();
        } catch (SdkException exception) {
            throw new IllegalStateException(
                    "AWS 자격 증명을 찾지 못했습니다. AWS_ACCESS_KEY_ID 와 AWS_SECRET_ACCESS_KEY 를 확인하세요.",
                    exception);
        }
    }

    private IllegalStateException bucketNotFound(String bucket, SdkException cause) {
        return new IllegalStateException(
                "S3 버킷을 찾을 수 없습니다: bucket=%s, region=%s. 버킷명과 리전을 확인하세요."
                        .formatted(bucket, s3Properties.region()), cause);
    }
}
