package com.example.writegrow.domain.analysis.dto.response;

import com.example.writegrow.domain.analysis.entity.AnalysisStatus;
import com.example.writegrow.domain.analysis.entity.ErrorAnalysis;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 아동 화면용 응답. 확정 오류만 담는다.
 *
 * <p>낮은 확신도 후보는 개수조차 내려주지 않는다. 아동이 "숨겨진 오류가 더 있다"고 인지하는 것
 * 자체가 명세가 막으려는 상황이다.
 */
@Schema(description = "아동에게 전달하는 확정 오류 목록")
public record WritingErrorsResponse(

        @Schema(description = "글 ID", example = "1")
        Long writingId,

        @Schema(description = "분석 상태", example = "SUCCEEDED")
        AnalysisStatus status,

        @Schema(description = "분석 대상이 된 확정 텍스트", example = "오늘 학교에서 친구랑 놀앗다")
        String analyzedText,

        @Schema(description = "교정 대상으로 확정된 오류")
        List<ErrorCandidateResponse> errors,

        @Schema(description = "분석 시각", example = "2026-08-18T18:21:29")
        LocalDateTime analyzedAt,

        @Schema(description = "실패 사유. 실패했을 때만 값이 있다.")
        String failureReason
) {

    public static WritingErrorsResponse from(ErrorAnalysis analysis) {
        return new WritingErrorsResponse(
                analysis.getWritingId(),
                analysis.getStatus(),
                analysis.getAnalyzedText(),
                analysis.confirmedCandidates().stream().map(ErrorCandidateResponse::from).toList(),
                analysis.getCompletedAt(),
                analysis.getFailureReason());
    }
}
