package com.example.writegrow.infra.s3;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.writegrow.support.S3PropertiesFixtures;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("StorageKeyFactory 단위 테스트")
class StorageKeyFactoryTest {

    private static final Long WRITING_ID = 100L;
    private static final String TODAY = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd"));

    private StorageKeyFactory factory(String keyPrefix) {
        return new StorageKeyFactory(S3PropertiesFixtures.withKeyPrefix(keyPrefix));
    }

    @Test
    @DisplayName("이미지 키는 환경 접두사와 날짜, 글 ID 순으로 만들어진다")
    void buildsImageKey() {
        String key = factory("dev/").handwritingImageKey(WRITING_ID, "png");

        assertThat(key)
                .startsWith("dev/handwriting/%s/%d/image-".formatted(TODAY, WRITING_ID))
                .endsWith(".png");
    }

    @Test
    @DisplayName("획 데이터 키도 같은 접두사 아래에 만들어진다")
    void buildsStrokeDataKey() {
        String key = factory("dev/").strokeDataKey(WRITING_ID);

        assertThat(key)
                .startsWith("dev/handwriting/%s/%d/strokes-".formatted(TODAY, WRITING_ID))
                .endsWith(".json");
    }

    @Test
    @DisplayName("접두사에 슬래시를 빠뜨려도 경로가 붙어버리지 않는다")
    void normalizesPrefixWithoutTrailingSlash() {
        String key = factory("prod").strokeDataKey(WRITING_ID);

        assertThat(key).startsWith("prod/handwriting/");
    }

    @Test
    @DisplayName("접두사가 없으면 붙이지 않는다")
    void allowsEmptyPrefix() {
        String key = factory("").strokeDataKey(WRITING_ID);

        assertThat(key).startsWith("handwriting/");
    }

    @Test
    @DisplayName("dev 와 prod 는 서로 다른 경로에 쓴다")
    void separatesEnvironments() {
        String dev = factory("dev/").handwritingImageKey(WRITING_ID, "png");
        String prod = factory("prod/").handwritingImageKey(WRITING_ID, "png");

        assertThat(dev).doesNotStartWith("prod/");
        assertThat(prod).doesNotStartWith("dev/");
    }
}
