package com.example.writegrow.infra.s3;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 손글씨 원본 자산의 S3 키 규칙.
 *
 * <pre>
 * handwriting/{yyyy}/{MM}/{dd}/{writingId}/image-{uuid}.{ext}
 * handwriting/{yyyy}/{MM}/{dd}/{writingId}/strokes-{uuid}.json
 * </pre>
 */
public final class StorageKeyFactory {

    private static final DateTimeFormatter DATE_PATH = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private StorageKeyFactory() {
    }

    public static String handwritingImageKey(Long writingId, String extension) {
        return "%s/image-%s.%s".formatted(prefix(writingId), UUID.randomUUID(), extension);
    }

    public static String strokeDataKey(Long writingId) {
        return "%s/strokes-%s.json".formatted(prefix(writingId), UUID.randomUUID());
    }

    private static String prefix(Long writingId) {
        return "handwriting/%s/%d".formatted(LocalDate.now().format(DATE_PATH), writingId);
    }
}
