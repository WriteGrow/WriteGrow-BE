package com.example.writegrow.domain.analysis.dto.response;

import com.example.writegrow.domain.analysis.entity.AnalysisStatus;
import com.example.writegrow.domain.analysis.entity.OcrResult;
import com.example.writegrow.domain.handwriting.dto.response.HandwritingSummaryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "손글씨 분석 결과 조회 응답")
public record AnalysisResponse(

        @Schema(description = "글 ID", example = "1")
        Long writingId,

        @Schema(description = "분석 상태", example = "SUCCEEDED")
        AnalysisStatus status,

        @Schema(description = "변환된 전체 텍스트", example = "오늘 학교에서 친구랑 놀앗다")
        String fullText,

        @Schema(description = "전체 확신도", example = "0.88")
        Double overallConfidence,

        @Schema(description = "분석 제공자", example = "writegrow-ai")
        String provider,

        @Schema(description = "분석 요청 시각", example = "2026-08-09T20:14:05")
        LocalDateTime requestedAt,

        @Schema(description = "분석 완료 시각", example = "2026-08-09T20:14:22")
        LocalDateTime completedAt,

        @Schema(description = "실패 사유. 상태가 FAILED 일 때만 채워진다.")
        String failureReason,

        @Schema(description = "구절별 변환 결과와 확신도")
        List<OcrSegmentResponse> segments,

        @Schema(description = "작성 과정 분석 지표")
        ProcessMetricResponse processMetric,

        @Schema(description = "손글씨 원본 정보")
        HandwritingSummaryResponse handwriting
) {

    public static AnalysisResponse of(OcrResult ocrResult,
                                      ProcessMetricResponse processMetric,
                                      HandwritingSummaryResponse handwriting) {
        return new AnalysisResponse(
                ocrResult.getWritingId(),
                ocrResult.getStatus(),
                ocrResult.getFullText(),
                ocrResult.getOverallConfidence(),
                ocrResult.getProvider(),
                ocrResult.getRequestedAt(),
                ocrResult.getCompletedAt(),
                ocrResult.getFailureReason(),
                ocrResult.getSegments().stream().map(OcrSegmentResponse::from).toList(),
                processMetric,
                handwriting);
    }
}
