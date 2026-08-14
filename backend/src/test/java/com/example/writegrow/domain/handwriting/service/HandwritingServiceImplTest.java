package com.example.writegrow.domain.handwriting.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.writegrow.domain.handwriting.dto.request.StrokeBatchAppendRequest;
import com.example.writegrow.domain.handwriting.dto.response.HandwritingAnalysisSource;
import com.example.writegrow.domain.handwriting.dto.response.StrokeBatchAppendResponse;
import com.example.writegrow.domain.handwriting.entity.HandwritingAsset;
import com.example.writegrow.domain.handwriting.entity.StrokeBatch;
import com.example.writegrow.domain.handwriting.entity.StrokeData;
import com.example.writegrow.domain.handwriting.entity.StrokePayload;
import com.example.writegrow.domain.handwriting.entity.StrokePoint;
import com.example.writegrow.domain.handwriting.exception.HandwritingErrorCode;
import com.example.writegrow.domain.handwriting.exception.HandwritingException;
import com.example.writegrow.domain.handwriting.repository.HandwritingAssetRepository;
import com.example.writegrow.domain.handwriting.repository.StrokeBatchRepository;
import com.example.writegrow.domain.writing.entity.Writing;
import com.example.writegrow.domain.writing.exception.WritingErrorCode;
import com.example.writegrow.domain.writing.exception.WritingException;
import com.example.writegrow.domain.writing.repository.WritingRepository;
import com.example.writegrow.infra.s3.StorageClient;
import com.example.writegrow.infra.s3.StorageKeyFactory;
import com.example.writegrow.support.S3PropertiesFixtures;
import com.example.writegrow.support.WritingFixtures;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
@DisplayName("HandwritingServiceImpl 단위 테스트")
class HandwritingServiceImplTest {

    private static final Long PROFILE_ID = 1L;
    private static final Long WRITING_ID = 100L;

    @Mock
    private StrokeBatchRepository strokeBatchRepository;

    @Mock
    private HandwritingAssetRepository handwritingAssetRepository;

    @Mock
    private WritingRepository writingRepository;

    @Mock
    private StorageClient storageClient;

    private HandwritingServiceImpl handwritingService;

    @BeforeEach
    void setUp() {
        // 직렬화 결과와 키 규칙 자체가 검증 대상이므로 ObjectMapper 와 StorageKeyFactory 는 실제 객체를 쓴다.
        handwritingService = new HandwritingServiceImpl(
                strokeBatchRepository, handwritingAssetRepository, writingRepository,
                storageClient, new StorageKeyFactory(S3PropertiesFixtures.withKeyPrefix("test/")),
                new ObjectMapper());
    }

    @Nested
    @DisplayName("획 데이터 수집")
    class AppendStrokeBatch {

        @Test
        @DisplayName("새 배치는 저장되고 전체 배치 수가 응답된다")
        void savesNewBatch() {
            givenPenWriting();
            given(strokeBatchRepository.existsByWritingIdAndBatchSeq(WRITING_ID, 0)).willReturn(false);
            given(strokeBatchRepository.save(any(StrokeBatch.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(strokeBatchRepository.countByWritingId(WRITING_ID)).willReturn(1L);

            StrokeBatchAppendResponse response = handwritingService.appendStrokeBatch(
                    PROFILE_ID, WRITING_ID, new StrokeBatchAppendRequest(0, List.of(stroke(0))));

            assertThat(response.duplicated()).isFalse();
            assertThat(response.strokeCount()).isEqualTo(1);
            assertThat(response.totalBatches()).isEqualTo(1L);
        }

        @Test
        @DisplayName("같은 배치 순번을 다시 받으면 저장하지 않고 duplicated 를 반환한다")
        void isIdempotent() {
            givenPenWriting();
            given(strokeBatchRepository.existsByWritingIdAndBatchSeq(WRITING_ID, 3)).willReturn(true);
            given(strokeBatchRepository.countByWritingId(WRITING_ID)).willReturn(4L);

            StrokeBatchAppendResponse response = handwritingService.appendStrokeBatch(
                    PROFILE_ID, WRITING_ID, new StrokeBatchAppendRequest(3, List.of(stroke(0))));

            assertThat(response.duplicated()).isTrue();
            assertThat(response.totalBatches()).isEqualTo(4L);
            verify(strokeBatchRepository, never()).save(any(StrokeBatch.class));
        }

        @Test
        @DisplayName("키보드 글에는 획 데이터를 보낼 수 없다")
        void rejectsKeyboardWriting() {
            Writing writing = WritingFixtures.keyboardWriting(WRITING_ID, PROFILE_ID);
            given(writingRepository.findById(WRITING_ID)).willReturn(Optional.of(writing));

            assertThatThrownBy(() -> handwritingService.appendStrokeBatch(
                    PROFILE_ID, WRITING_ID, new StrokeBatchAppendRequest(0, List.of(stroke(0)))))
                    .isInstanceOf(HandwritingException.class)
                    .extracting(exception -> ((HandwritingException) exception).getErrorCode())
                    .isEqualTo(HandwritingErrorCode.NOT_HANDWRITING);
        }

        @Test
        @DisplayName("다른 아동의 글에는 획 데이터를 보낼 수 없다")
        void rejectsOtherProfile() {
            Writing writing = WritingFixtures.penWriting(WRITING_ID, 999L);
            given(writingRepository.findById(WRITING_ID)).willReturn(Optional.of(writing));

            assertThatThrownBy(() -> handwritingService.appendStrokeBatch(
                    PROFILE_ID, WRITING_ID, new StrokeBatchAppendRequest(0, List.of(stroke(0)))))
                    .isInstanceOf(WritingException.class)
                    .extracting(exception -> ((WritingException) exception).getErrorCode())
                    .isEqualTo(WritingErrorCode.FORBIDDEN_PROFILE);
        }
    }

    @Nested
    @DisplayName("손글씨 이미지 업로드")
    class UploadImage {

        @Test
        @DisplayName("PNG 이미지를 올리면 S3 에 저장하고 자산 레코드를 만든다")
        void uploadsPng() {
            givenPenWriting();
            given(handwritingAssetRepository.findByWritingId(WRITING_ID)).willReturn(Optional.empty());
            given(storageClient.upload(anyString(), any(byte[].class), eq(MediaType.IMAGE_PNG_VALUE)))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(storageClient.presignedGetUrl(anyString())).willReturn("https://s3.example/image.png");
            given(handwritingAssetRepository.save(any(HandwritingAsset.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            var response = handwritingService.uploadImage(PROFILE_ID, WRITING_ID,
                    new MockMultipartFile("file", "a.png", MediaType.IMAGE_PNG_VALUE, new byte[]{1, 2, 3}),
                    1024, 768);

            assertThat(response.imageUrl()).isEqualTo("https://s3.example/image.png");
        }

        @Test
        @DisplayName("빈 파일은 거부한다")
        void rejectsEmptyFile() {
            givenPenWriting();

            assertThatThrownBy(() -> handwritingService.uploadImage(PROFILE_ID, WRITING_ID,
                    new MockMultipartFile("file", "a.png", MediaType.IMAGE_PNG_VALUE, new byte[0]),
                    1024, 768))
                    .isInstanceOf(HandwritingException.class)
                    .extracting(exception -> ((HandwritingException) exception).getErrorCode())
                    .isEqualTo(HandwritingErrorCode.EMPTY_IMAGE);
        }

        @Test
        @DisplayName("PNG/JPEG 가 아닌 형식은 거부한다")
        void rejectsUnsupportedType() {
            givenPenWriting();

            assertThatThrownBy(() -> handwritingService.uploadImage(PROFILE_ID, WRITING_ID,
                    new MockMultipartFile("file", "a.gif", MediaType.IMAGE_GIF_VALUE, new byte[]{1}),
                    1024, 768))
                    .isInstanceOf(HandwritingException.class)
                    .extracting(exception -> ((HandwritingException) exception).getErrorCode())
                    .isEqualTo(HandwritingErrorCode.UNSUPPORTED_IMAGE_TYPE);
        }
    }

    @Nested
    @DisplayName("분석용 자료 준비")
    class FinalizeForAnalysis {

        @Test
        @DisplayName("획을 병합해 S3 에 올리고 요약 지표를 자산에 반영한다")
        void mergesAndUploads() {
            HandwritingAsset asset = HandwritingAsset.create(WRITING_ID);
            asset.updateImage("handwriting/image.png", 1024, 768);
            given(handwritingAssetRepository.findByWritingId(WRITING_ID)).willReturn(Optional.of(asset));
            given(strokeBatchRepository.findAllByWritingIdOrderByBatchSeqAsc(WRITING_ID)).willReturn(List.of(
                    StrokeBatch.of(WRITING_ID, 0, StrokePayload.of(List.of(stroke(0)))),
                    StrokeBatch.of(WRITING_ID, 1, StrokePayload.of(List.of(stroke(1))))));
            given(storageClient.upload(anyString(), any(byte[].class), eq("application/json")))
                    .willAnswer(invocation -> invocation.getArgument(0));
            given(storageClient.presignedGetUrl(anyString())).willReturn("https://s3.example/object");
            given(handwritingAssetRepository.save(any(HandwritingAsset.class)))
                    .willAnswer(invocation -> invocation.getArgument(0));

            HandwritingAnalysisSource source = handwritingService.finalizeForAnalysis(WRITING_ID);

            assertThat(source.strokeCount()).isEqualTo(2);
            assertThat(source.imageUrl()).isEqualTo("https://s3.example/object");
            assertThat(source.strokeDataUrl()).isEqualTo("https://s3.example/object");
            assertThat(asset.hasStrokeData()).isTrue();
            assertThat(asset.getStrokeCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("이미지가 아직 없으면 IMAGE_REQUIRED 예외가 발생한다")
        void requiresImage() {
            given(handwritingAssetRepository.findByWritingId(WRITING_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> handwritingService.finalizeForAnalysis(WRITING_ID))
                    .isInstanceOf(HandwritingException.class)
                    .extracting(exception -> ((HandwritingException) exception).getErrorCode())
                    .isEqualTo(HandwritingErrorCode.IMAGE_REQUIRED);
        }

        @Test
        @DisplayName("획 데이터가 하나도 없으면 STROKE_DATA_REQUIRED 예외가 발생한다")
        void requiresStrokeData() {
            HandwritingAsset asset = HandwritingAsset.create(WRITING_ID);
            asset.updateImage("handwriting/image.png", 1024, 768);
            given(handwritingAssetRepository.findByWritingId(WRITING_ID)).willReturn(Optional.of(asset));
            given(strokeBatchRepository.findAllByWritingIdOrderByBatchSeqAsc(WRITING_ID)).willReturn(List.of());

            assertThatThrownBy(() -> handwritingService.finalizeForAnalysis(WRITING_ID))
                    .isInstanceOf(HandwritingException.class)
                    .extracting(exception -> ((HandwritingException) exception).getErrorCode())
                    .isEqualTo(HandwritingErrorCode.STROKE_DATA_REQUIRED);
        }
    }

    private void givenPenWriting() {
        given(writingRepository.findById(WRITING_ID))
                .willReturn(Optional.of(WritingFixtures.penWriting(WRITING_ID, PROFILE_ID)));
    }

    private static StrokeData stroke(int index) {
        long penDownAt = index * 1000L;
        return new StrokeData(index, penDownAt, penDownAt + 400,
                List.of(new StrokePoint(10.0, 20.0, penDownAt, 0.5)));
    }
}
