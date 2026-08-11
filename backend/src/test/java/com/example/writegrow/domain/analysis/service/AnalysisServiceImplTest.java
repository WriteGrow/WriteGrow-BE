package com.example.writegrow.domain.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.writegrow.domain.analysis.entity.OcrResult;
import com.example.writegrow.domain.analysis.exception.AnalysisErrorCode;
import com.example.writegrow.domain.analysis.exception.AnalysisException;
import com.example.writegrow.domain.analysis.repository.OcrResultRepository;
import com.example.writegrow.domain.analysis.repository.WritingProcessMetricRepository;
import com.example.writegrow.domain.handwriting.dto.response.HandwritingAnalysisSource;
import com.example.writegrow.domain.handwriting.service.HandwritingQueryService;
import com.example.writegrow.domain.handwriting.service.HandwritingService;
import com.example.writegrow.domain.writing.entity.Writing;
import com.example.writegrow.domain.writing.entity.WritingStatus;
import com.example.writegrow.domain.writing.event.HandwritingSubmittedEvent;
import com.example.writegrow.domain.writing.repository.WritingRepository;
import com.example.writegrow.infra.ai.AiAnalysisClient;
import com.example.writegrow.infra.ai.dto.AiAnalysisRequest;
import com.example.writegrow.infra.ai.dto.AiAnalysisResponse;
import com.example.writegrow.infra.ai.exception.AiAnalysisErrorCode;
import com.example.writegrow.infra.ai.exception.AiAnalysisException;
import com.example.writegrow.support.WritingFixtures;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalysisServiceImpl 단위 테스트")
class AnalysisServiceImplTest {

    private static final Long PROFILE_ID = 1L;
    private static final Long WRITING_ID = 100L;

    @Mock
    private AnalysisResultService analysisResultService;

    @Mock
    private HandwritingService handwritingService;

    @Mock
    private HandwritingQueryService handwritingQueryService;

    @Mock
    private OcrResultRepository ocrResultRepository;

    @Mock
    private WritingProcessMetricRepository writingProcessMetricRepository;

    @Mock
    private WritingRepository writingRepository;

    @Mock
    private AiAnalysisClient aiAnalysisClient;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private AnalysisServiceImpl analysisService;

    @Nested
    @DisplayName("분석 파이프라인 실행")
    class RunAnalysis {

        @Test
        @DisplayName("성공하면 결과를 반영한다")
        void completesAnalysis() {
            AiAnalysisResponse response = new AiAnalysisResponse("오늘 학교에서 친구랑 놀앗다", 0.88, List.of(), null);
            given(writingRepository.findById(WRITING_ID))
                    .willReturn(Optional.of(WritingFixtures.penWriting(WRITING_ID, PROFILE_ID)));
            given(handwritingService.finalizeForAnalysis(WRITING_ID)).willReturn(analysisSource());
            given(aiAnalysisClient.analyze(any(AiAnalysisRequest.class))).willReturn(response);
            given(aiAnalysisClient.provider()).willReturn("stub");

            analysisService.runAnalysis(WRITING_ID, PROFILE_ID);

            verify(analysisResultService).markProcessing(WRITING_ID, PROFILE_ID);
            verify(analysisResultService).completeAnalysis(WRITING_ID, PROFILE_ID, "stub", response);
            verify(analysisResultService, never()).failAnalysis(anyLong(), anyLong(), anyString());
        }

        @Test
        @DisplayName("AI 호출이 실패해도 예외를 밖으로 던지지 않고 실패 상태로 기록한다")
        void recordsFailureWithoutPropagating() {
            given(writingRepository.findById(WRITING_ID))
                    .willReturn(Optional.of(WritingFixtures.penWriting(WRITING_ID, PROFILE_ID)));
            given(handwritingService.finalizeForAnalysis(WRITING_ID)).willReturn(analysisSource());
            willThrow(new AiAnalysisException(AiAnalysisErrorCode.AI_CALL_FAILED))
                    .given(aiAnalysisClient).analyze(any(AiAnalysisRequest.class));

            assertThatCode(() -> analysisService.runAnalysis(WRITING_ID, PROFILE_ID))
                    .doesNotThrowAnyException();

            verify(analysisResultService).failAnalysis(eq(WRITING_ID), eq(PROFILE_ID), anyString());
            verify(analysisResultService, never())
                    .completeAnalysis(anyLong(), anyLong(), anyString(), any());
        }

        @Test
        @DisplayName("획 데이터 준비가 실패해도 실패 상태로만 기록하고 원본을 지우지 않는다")
        void keepsOriginalOnPreparationFailure() {
            given(writingRepository.findById(WRITING_ID))
                    .willReturn(Optional.of(WritingFixtures.penWriting(WRITING_ID, PROFILE_ID)));
            willThrow(new IllegalStateException("획 데이터 없음"))
                    .given(handwritingService).finalizeForAnalysis(WRITING_ID);

            analysisService.runAnalysis(WRITING_ID, PROFILE_ID);

            verify(analysisResultService).failAnalysis(eq(WRITING_ID), eq(PROFILE_ID), anyString());
            // 원본 삭제 API 자체를 호출하지 않는다. 손글씨 자산은 재시도를 위해 그대로 남는다.
            verify(aiAnalysisClient, never()).analyze(any(AiAnalysisRequest.class));
        }
    }

    @Nested
    @DisplayName("재시도")
    class RetryAnalysis {

        @Test
        @DisplayName("실패한 분석은 다시 시작할 수 있다")
        void retriesFailedAnalysis() {
            Writing writing = WritingFixtures.penWriting(WRITING_ID, PROFILE_ID);
            writing.submitHandwriting();
            writing.markAnalysisFailed();

            OcrResult ocrResult = OcrResult.create(WRITING_ID);
            ocrResult.markFailed("AI 서버 연결 실패");

            given(writingRepository.findById(WRITING_ID)).willReturn(Optional.of(writing));
            given(ocrResultRepository.findByWritingId(WRITING_ID)).willReturn(Optional.of(ocrResult));

            analysisService.retryAnalysis(PROFILE_ID, WRITING_ID);

            assertThat(writing.getStatus()).isEqualTo(WritingStatus.SUBMITTED);
            verify(eventPublisher).publishEvent(new HandwritingSubmittedEvent(WRITING_ID, PROFILE_ID));
        }

        @Test
        @DisplayName("실패 상태가 아니면 NOT_FAILED_STATE 예외가 발생한다")
        void rejectsNonFailedState() {
            Writing writing = WritingFixtures.penWriting(WRITING_ID, PROFILE_ID);
            writing.submitHandwriting();

            OcrResult ocrResult = OcrResult.create(WRITING_ID);
            ocrResult.markSucceeded("오늘 학교에서 친구랑 놀앗다", 0.88, "stub");

            given(writingRepository.findById(WRITING_ID)).willReturn(Optional.of(writing));
            given(ocrResultRepository.findByWritingId(WRITING_ID)).willReturn(Optional.of(ocrResult));

            assertThatThrownBy(() -> analysisService.retryAnalysis(PROFILE_ID, WRITING_ID))
                    .isInstanceOf(AnalysisException.class)
                    .extracting(exception -> ((AnalysisException) exception).getErrorCode())
                    .isEqualTo(AnalysisErrorCode.NOT_FAILED_STATE);
        }
    }

    @Nested
    @DisplayName("결과 조회")
    class GetAnalysis {

        @Test
        @DisplayName("분석 기록이 없으면 ANALYSIS_NOT_FOUND 예외가 발생한다")
        void throwsWhenMissing() {
            given(writingRepository.findById(WRITING_ID))
                    .willReturn(Optional.of(WritingFixtures.penWriting(WRITING_ID, PROFILE_ID)));
            given(ocrResultRepository.findWithSegmentsByWritingId(WRITING_ID)).willReturn(Optional.empty());

            assertThatThrownBy(() -> analysisService.getAnalysis(PROFILE_ID, WRITING_ID))
                    .isInstanceOf(AnalysisException.class)
                    .extracting(exception -> ((AnalysisException) exception).getErrorCode())
                    .isEqualTo(AnalysisErrorCode.ANALYSIS_NOT_FOUND);
        }

        @Test
        @DisplayName("과정 지표가 아직 없어도 조회는 성공한다")
        void returnsWithoutProcessMetric() {
            OcrResult ocrResult = OcrResult.create(WRITING_ID);
            ocrResult.markSucceeded("오늘 학교에서 친구랑 놀앗다", 0.88, "stub");

            given(writingRepository.findById(WRITING_ID))
                    .willReturn(Optional.of(WritingFixtures.penWriting(WRITING_ID, PROFILE_ID)));
            given(ocrResultRepository.findWithSegmentsByWritingId(WRITING_ID)).willReturn(Optional.of(ocrResult));
            given(writingProcessMetricRepository.findByWritingId(WRITING_ID)).willReturn(Optional.empty());
            given(handwritingQueryService.findByWritingId(WRITING_ID)).willReturn(Optional.empty());

            var response = analysisService.getAnalysis(PROFILE_ID, WRITING_ID);

            assertThat(response.fullText()).isEqualTo("오늘 학교에서 친구랑 놀앗다");
            assertThat(response.processMetric()).isNull();
            assertThat(response.handwriting()).isNull();
        }
    }

    private static HandwritingAnalysisSource analysisSource() {
        return new HandwritingAnalysisSource(
                WRITING_ID, "https://s3.example/image.png", "https://s3.example/strokes.json",
                1024, 768, 48, 92000L);
    }
}
