package com.example.writegrow.global.config;

import com.example.writegrow.global.config.properties.S3Properties;
import java.net.URI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@Configuration
public class S3Config {

    /**
     * 클라이언트와 presigner 가 같은 provider 를 공유한다. 따로 만들면 자격 증명 캐시와 갱신 스레드가
     * 두 벌 생기고, 기동 시 확인({@code S3BucketVerifier})이 검사한 것과 다른 인스턴스가 된다.
     */
    @Bean
    public AwsCredentialsProvider awsCredentialsProvider(S3Properties properties) {
        if (properties.hasStaticCredentials()) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(properties.accessKey(), properties.secretKey()));
        }
        return DefaultCredentialsProvider.create();
    }

    @Bean
    public S3Client s3Client(S3Properties properties, AwsCredentialsProvider credentialsProvider) {
        var builder = S3Client.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.pathStyleAccess())
                        .build());

        if (properties.hasCustomEndpoint()) {
            builder.endpointOverride(URI.create(properties.endpoint()));
        }
        return builder.build();
    }

    @Bean
    public S3Presigner s3Presigner(S3Properties properties, AwsCredentialsProvider credentialsProvider) {
        var builder = S3Presigner.builder()
                .region(Region.of(properties.region()))
                .credentialsProvider(credentialsProvider)
                .serviceConfiguration(S3Configuration.builder()
                        .pathStyleAccessEnabled(properties.pathStyleAccess())
                        .build());

        if (properties.hasCustomEndpoint()) {
            builder.endpointOverride(URI.create(properties.endpoint()));
        }
        return builder.build();
    }
}
