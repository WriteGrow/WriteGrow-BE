package com.example.writegrow.infra.s3;

import java.time.Duration;

/**
 * 파일 저장소 포트. 도메인 서비스는 이 인터페이스만 알고, 실제 구현(S3)은 infra 에 격리된다.
 */
public interface StorageClient {

    /**
     * 객체를 업로드하고 저장된 키를 돌려준다.
     *
     * @param key         저장소 내 객체 키
     * @param content     업로드할 바이트
     * @param contentType MIME 타입
     */
    String upload(String key, byte[] content, String contentType);

    /**
     * 읽기용 presigned URL 을 발급한다. AI 서버에 원본을 전달할 때 사용한다.
     */
    String presignedGetUrl(String key, Duration duration);

    /**
     * 기본 만료 시간으로 읽기용 presigned URL 을 발급한다.
     */
    String presignedGetUrl(String key);
}
