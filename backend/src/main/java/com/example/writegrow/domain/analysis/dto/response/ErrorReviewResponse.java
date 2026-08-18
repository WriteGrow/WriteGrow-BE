package com.example.writegrow.domain.analysis.dto.response;

import com.example.writegrow.domain.analysis.entity.AnalysisStatus;
import com.example.writegrow.domain.analysis.entity.ErrorAnalysis;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 보호자·교사 검토 화면용 응답.
 *
 * <p>확정 오류 개수를 함께 내려주는 것은, 검토 대상과 확정 오류를 <b>구분해서</b> 보여줘야 한다는
 * 명세 표시 규칙 때문이다. 개수를 알아야 "검토 대상 4건 / 확정 오류 12건" 같은 대비가 가능하다.
 */
@Schema(description = "낮은 확신도 오류 검토 대상")
public record ErrorReviewResponse(

        @Schema(description = "글 ID", example = "1")
        Long writingId,

        @Schema(description = "분석 상태", example = "SUCCEEDED")
        AnalysisStatus status,

        @Schema(description = "분석 대상이 된 확정 텍스트", example = "오늘 학교에서 친구랑 놀앗다")
        String analyzedText,

        @Schema(description = "검토 대상 후보 수", example = "1")
        int reviewCount,

        @Schema(description = "확정 오류 수. 검토 대상과 구분해 보여주기 위한 값이다.", example = "1")
        int confirmedCount,

        @Schema(description = "낮은 확신도 후보. 아동에게는 노출되지 않았고 통계에도 반영되지 않았다.")
        List<ErrorCandidateResponse> reviewCandidates,

        @Schema(description = "분석 시각", example = "2026-08-18T18:21:29")
        LocalDateTime analyzedAt
) {

    public static ErrorReviewResponse from(ErrorAnalysis analysis) {
        List<ErrorCandidateResponse> candidates = analysis.reviewCandidates().stream()
                .map(ErrorCandidateResponse::from)
                .toList();

        return new ErrorReviewResponse(
                analysis.getWritingId(),
                analysis.getStatus(),
                analysis.getAnalyzedText(),
                candidates.size(),
                analysis.confirmedCandidates().size(),
                candidates,
                analysis.getCompletedAt());
    }
}
