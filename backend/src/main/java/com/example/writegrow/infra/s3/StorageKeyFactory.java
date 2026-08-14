package com.example.writegrow.infra.s3;

import com.example.writegrow.global.config.properties.S3Properties;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 손글씨 원본 자산의 S3 키 규칙.
 *
 * <pre>
 * {prefix}handwriting/{yyyy}/{MM}/{dd}/{writingId}/image-{uuid}.{ext}
 * {prefix}handwriting/{yyyy}/{MM}/{dd}/{writingId}/strokes-{uuid}.json
 * </pre>
 *
 * <p>{@code prefix} 는 dev 와 prod 가 버킷 하나를 나눠 쓰기 위한 환경 구분자다
 * ({@code writegrow.s3.key-prefix}). 키를 만드는 곳이 여기 하나뿐이므로 구분자도 여기서만 붙인다.
 */
@Component
@RequiredArgsConstructor
public class StorageKeyFactory {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final S3Properties s3Properties;

    public String handwritingImageKey(Long writingId, String extension) {
        return "%s/image-%s.%s".formatted(prefix(writingId), UUID.randomUUID(), extension);
    }

    public String strokeDataKey(Long writingId) {
        return "%s/strokes-%s.json".formatted(prefix(writingId), UUID.randomUUID());
    }

    private String prefix(Long writingId) {
        return "%shandwriting/%s/%d".formatted(
                s3Properties.normalizedKeyPrefix(), LocalDate.now().format(DATE_PATH), writingId);
    }
}
