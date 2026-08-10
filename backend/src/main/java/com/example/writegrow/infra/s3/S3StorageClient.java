package com.example.writegrow.infra.s3;

import com.example.writegrow.global.config.properties.S3Properties;
import com.example.writegrow.infra.s3.exception.StorageErrorCode;
import com.example.writegrow.infra.s3.exception.StorageException;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class S3StorageClient implements StorageClient {

    private static final Duration FALLBACK_PRESIGN_DURATION = Duration.ofMinutes(10);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final S3Properties s3Properties;

    @Override
    public String upload(String key, byte[] content, String contentType) {
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(s3Properties.bucket())
                            .key(key)
                            .contentType(contentType)
                            .contentLength((long) content.length)
                            .build(),
                    RequestBody.fromBytes(content));
            return key;
        } catch (RuntimeException exception) {
            log.error("S3 업로드 실패: bucket={}, key={}", s3Properties.bucket(), key, exception);
            throw new StorageException(StorageErrorCode.UPLOAD_FAILED, exception);
        }
    }

    @Override
    public String presignedGetUrl(String key, Duration duration) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Properties.bucket())
                    .key(key)
                    .build();

            return s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                            .signatureDuration(duration)
                            .getObjectRequest(getObjectRequest)
                            .build())
                    .url()
                    .toExternalForm();
        } catch (RuntimeException exception) {
            log.error("S3 presigned URL 발급 실패: bucket={}, key={}", s3Properties.bucket(), key, exception);
            throw new StorageException(StorageErrorCode.PRESIGN_FAILED, exception);
        }
    }

    @Override
    public String presignedGetUrl(String key) {
        Duration duration = s3Properties.presignDuration() != null
                ? s3Properties.presignDuration()
                : FALLBACK_PRESIGN_DURATION;
        return presignedGetUrl(key, duration);
    }
}
