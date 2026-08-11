package com.example.writegrow.domain.analysis.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.writegrow.domain.activity.entity.ActivityEventType;
import com.example.writegrow.domain.activity.service.ActivityEventService;
import com.example.writegrow.domain.analysis.entity.AnalysisStatus;
import com.example.writegrow.domain.analysis.entity.OcrResult;
import com.example.writegrow.domain.analysis.entity.OcrSegment;
import com.example.writegrow.domain.analysis.entity.WritingProcessMetric;
import com.example.writegrow.domain.analysis.repository.OcrResultRepository;
import com.example.writegrow.domain.analysis.repository.WritingProcessMetricRepository;
import com.example.writegrow.domain.writing.entity.Writing;
import com.example.writegrow.domain.writing.entity.WritingStatus;
import com.example.writegrow.domain.writing.repository.WritingRepository;
import com.example.writegrow.global.config.properties.OcrProperties;
import com.example.writegrow.infra.ai.dto.AiAnalysisResponse;
import com.example.writegrow.infra.ai.dto.AiAnalysisResponse.HesitationPoint;
import com.example.writegrow.infra.ai.dto.AiAnalysisResponse.ProcessMetric;
import com.example.writegrow.infra.ai.dto.AiAnalysisResponse.Segment;
import com.example.writegrow.support.WritingFixtures;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnalysisResultServiceImpl 단위 테스트")
class AnalysisResultServiceImplTest {

    private static final Long PROFILE_ID = 1L;
    private static final Long WRITING_ID = 100L;
    private static final double THRESHOLD = 0.7;

    @Mock
    private OcrResultRepository ocrResultRepository;

    @Mock
    private WritingProcessMetricRepository writingProcessMetricRepository;

    @Mock
    private WritingRepository writingRepository;

    @Mock
    private ActivityEventService activityEventService;

    private AnalysisResultServiceImpl analysisResultService;

    @BeforeEach
    void setUp() {
        analysisResultService = new AnalysisResultServiceImpl(
                ocrResultRepository, writingProcessMetricRepository, writingRepository,
                activityEventService, new OcrProperties(THRESHOLD));
    }

    @Nested
    @DisplayName("분석 성공 반영")
    class CompleteAnalysis {

        @Test
        @DisplayName("확신도가 임계값 미만인 구절만 아동 교정 대상에서 제외 표시된다")
        void marksLowConfidenceSegments() {
            OcrResult ocrResult = OcrResult.create(WRITING_ID);
            Writing writing = submittedPenWriting();
            given(ocrResultRepository.findWithSegmentsByWritingId(WRITING_ID)).willReturn(Optional.of(ocrResult));
            given(ocrResultRepository.save(any(OcrResult.class))).willAnswer(inv -> inv.getArgument(0));
            given(writingRepository.findWithRevisionsById(WRITING_ID)).willReturn(Optional.of(writing));

            analysisResultService.completeAnalysis(WRITING_ID, PROFILE_ID, "stub", new AiAnalysisResponse(
                    "오늘 학교에서 친구랑 놀앗다",
                    0.88,
                    List.of(
                            new Segment("경계값 아래", 0.69, 0, 3),
                            new Segment("경계값", 0.70, 4, 6),
                            new Segment("경계값 위", 0.71, 7, 10)),
                    null));

            assertThat(ocrResult.getSegments())
                    .extracting(OcrSegment::isLowConfidence)
                    .containsExactly(true, false, false);
        }

        @Test
        @DisplayName("확신도가 null 인 구절은 0.0 으로 보아 교정 대상에서 제외한다")
        void treatsNullConfidenceAsLow() {
            OcrResult ocrResult = OcrResult.create(WRITING_ID);
            given(ocrResultRepository.findWithSegmentsByWritingId(WRITING_ID)).willReturn(Optional.of(ocrResult));
            given(ocrResultRepository.save(any(OcrResult.class))).willAnswer(inv -> inv.getArgument(0));
            given(writingRepository.findWithRevisionsById(WRITING_ID))
                    .willReturn(Optional.of(submittedPenWriting()));

            analysisResultService.completeAnalysis(WRITING_ID, PROFILE_ID, "stub", new AiAnalysisResponse(
                    "오늘 학교에서 친구랑 놀앗다", 0.5,
                    List.of(new Segment("확신도 없음", null, 0, 3)), null));

            assertThat(ocrResult.getSegments().getFirst().isLowConfidence()).isTrue();
        }

        @Test
        @DisplayName("변환 텍스트를 글에 반영하고 아동 확인 대기 상태로 만든다")
        void appliesOcrTextToWriting() {
            Writing writing = submittedPenWriting();
            given(ocrResultRepository.findWithSegmentsByWritingId(WRITING_ID))
                    .willReturn(Optional.of(OcrResult.create(WRITING_ID)));
            given(ocrResultRepository.save(any(OcrResult.class))).willAnswer(inv -> inv.getArgument(0));
            given(writingRepository.findWithRevisionsById(WRITING_ID)).willReturn(Optional.of(writing));

            analysisResultService.completeAnalysis(WRITING_ID, PROFILE_ID, "stub", new AiAnalysisResponse(
                    "오늘 학교에서 친구랑 놀앗다", 0.88, List.of(), null));

            assertThat(writing.getStatus()).isEqualTo(WritingStatus.ANALYZED);
            assertThat(writing.getOriginalText()).isEqualTo("오늘 학교에서 친구랑 놀앗다");
            verify(activityEventService).record(
                    eq(ActivityEventType.OCR_COMPLETED), eq(PROFILE_ID), eq(WRITING_ID), any());
        }

        @Test
        @DisplayName("과정 분석 지표(머뭇거림 지점 포함)를 저장한다")
        void savesProcessMetric() {
            given(ocrResultRepository.findWithSegmentsByWritingId(WRITING_ID))
                    .willReturn(Optional.of(OcrResult.create(WRITING_ID)));
            given(ocrResultRepository.save(any(OcrResult.class))).willAnswer(inv -> inv.getArgument(0));
            given(writingRepository.findWithRevisionsById(WRITING_ID))
                    .willReturn(Optional.of(submittedPenWriting()));
            given(writingProcessMetricRepository.findByWritingId(WRITING_ID)).willReturn(Optional.empty());
            given(writingProcessMetricRepository.save(any(WritingProcessMetric.class)))
                    .willAnswer(inv -> inv.getArgument(0));

            analysisResultService.completeAnalysis(WRITING_ID, PROFILE_ID, "stub", new AiAnalysisResponse(
                    "오늘 학교에서 친구랑 놀앗다", 0.88, List.of(),
                    new ProcessMetric(92000L, 4, 7300L, 410L,
                            List.of(new HesitationPoint(12, "놀", "ㄴ", 5200L, 2)))));

            ArgumentCaptor<WritingProcessMetric> captor = ArgumentCaptor.forClass(WritingProcessMetric.class);
            verify(writingProcessMetricRepository).save(captor.capture());

            WritingProcessMetric saved = captor.getValue();
            assertThat(saved.getTotalDurationMs()).isEqualTo(92000L);
            assertThat(saved.getLongestPauseMs()).isEqualTo(7300L);
            assertThat(saved.getHesitationPoints()).hasSize(1);
            assertThat(saved.getHesitationPoints().getFirst().character()).isEqualTo("놀");
            assertThat(saved.getHesitationPoints().getFirst().jamo()).isEqualTo("ㄴ");
        }
    }

    @Nested
    @DisplayName("분석 실패 반영")
    class FailAnalysis {

        @Test
        @DisplayName("실패 사유를 남기고 글을 재시도 가능한 상태로 만든다")
        void marksFailure() {
            OcrResult ocrResult = OcrResult.create(WRITING_ID);
            Writing writing = submittedPenWriting();
            given(ocrResultRepository.findWithSegmentsByWritingId(WRITING_ID)).willReturn(Optional.of(ocrResult));
            given(ocrResultRepository.save(any(OcrResult.class))).willAnswer(inv -> inv.getArgument(0));
            given(writingRepository.findById(WRITING_ID)).willReturn(Optional.of(writing));

            analysisResultService.failAnalysis(WRITING_ID, PROFILE_ID, "AI 서버 연결 실패");

            assertThat(ocrResult.getStatus()).isEqualTo(AnalysisStatus.FAILED);
            assertThat(ocrResult.getFailureReason()).isEqualTo("AI 서버 연결 실패");
            assertThat(writing.getStatus()).isEqualTo(WritingStatus.ANALYSIS_FAILED);
            verify(activityEventService).record(
                    eq(ActivityEventType.OCR_FAILED), eq(PROFILE_ID), eq(WRITING_ID), any());
        }
    }

    private static Writing submittedPenWriting() {
        Writing writing = WritingFixtures.penWriting(WRITING_ID, PROFILE_ID);
        writing.submitHandwriting();
        return writing;
    }
}
