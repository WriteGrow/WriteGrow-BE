package com.example.writegrow.infra.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.writegrow.support.S3PropertiesFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
@DisplayName("S3BucketVerifier 단위 테스트")
class S3BucketVerifierTest {

    private static final String BUCKET = S3PropertiesFixtures.BUCKET;

    @Mock
    private S3Client s3Client;

    @Mock
    private AwsCredentialsProvider credentialsProvider;

    private S3BucketVerifier verifier(String endpoint) {
        return new S3BucketVerifier(
                s3Client, credentialsProvider, S3PropertiesFixtures.withEndpoint(endpoint));
    }

    @Nested
    @DisplayName("접근 가능하면")
    class WhenAccessible {

        @Test
        @DisplayName("설정된 버킷으로 HeadBucket 을 호출하고 통과한다")
        void passes() {
            S3BucketVerifier verifier = verifier("");

            assertThatCode(verifier::verifyBucketAccessible).doesNotThrowAnyException();

            ArgumentCaptor<HeadBucketRequest> captor = ArgumentCaptor.forClass(HeadBucketRequest.class);
            verify(s3Client).headBucket(captor.capture());
            assertThat(captor.getValue().bucket()).isEqualTo(BUCKET);
        }
    }

    @Nested
    @DisplayName("접근할 수 없으면 기동을 막는다")
    class WhenNotAccessible {

        @Test
        @DisplayName("자격 증명을 못 찾으면 환경 변수 이름을 알려주고 버킷은 조회하지 않는다")
        void credentialsMissing() {
            given(credentialsProvider.resolveCredentials())
                    .willThrow(SdkClientException.create(
                            "Unable to load credentials from any of the providers in the chain"));

            assertThatThrownBy(verifier("")::verifyBucketAccessible)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("AWS_ACCESS_KEY_ID")
                    .hasMessageContaining("AWS_SECRET_ACCESS_KEY");

            verify(s3Client, never()).headBucket(any(HeadBucketRequest.class));
        }

        @Test
        @DisplayName("버킷이 없으면 버킷명과 리전을 알려준다")
        void bucketNotFound() {
            given(s3Client.headBucket(any(HeadBucketRequest.class)))
                    .willThrow(NoSuchBucketException.builder().message("not found").build());

            assertThatThrownBy(verifier("")::verifyBucketAccessible)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(BUCKET)
                    .hasMessageContaining("ap-northeast-2");
        }

        @Test
        @DisplayName("404 가 일반 S3Exception 으로 와도 버킷이 없다고 알려준다")
        void bucketNotFoundWithoutModeledException() {
            // HeadBucket 은 응답 본문이 없어 SDK 가 NoSuchBucketException 으로 해석하지 못할 수 있다.
            given(s3Client.headBucket(any(HeadBucketRequest.class)))
                    .willThrow(S3Exception.builder().statusCode(404).message("Not Found").build());

            assertThatThrownBy(verifier("")::verifyBucketAccessible)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("찾을 수 없습니다")
                    .hasMessageContaining(BUCKET);
        }

        @Test
        @DisplayName("권한이 없으면 IAM 권한을 확인하라고 알려준다")
        void accessDenied() {
            given(s3Client.headBucket(any(HeadBucketRequest.class)))
                    .willThrow(S3Exception.builder().statusCode(403).message("Forbidden").build());

            assertThatThrownBy(verifier("")::verifyBucketAccessible)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("403")
                    .hasMessageContaining("s3:ListBucket");
        }

        @Test
        @DisplayName("연결 자체가 안 되면 사용 중인 엔드포인트를 알려준다")
        void connectionFailed() {
            given(s3Client.headBucket(any(HeadBucketRequest.class)))
                    .willThrow(SdkClientException.create("connection refused"));

            assertThatThrownBy(verifier("http://localhost:9000")::verifyBucketAccessible)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("http://localhost:9000");
        }
    }
}
