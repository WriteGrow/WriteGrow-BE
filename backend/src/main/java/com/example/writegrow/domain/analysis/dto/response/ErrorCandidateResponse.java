package com.example.writegrow.domain.analysis.dto.response;

import com.example.writegrow.domain.analysis.entity.ErrorCandidate;
import com.example.writegrow.domain.analysis.entity.ErrorType;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "오류 후보 하나")
public record ErrorCandidateResponse(

        @Schema(description = "오류 유형", example = "FINAL_CONSONANT")
        ErrorType errorType,

        @Schema(description = "화면에 보여줄 유형 이름", example = "받침")
        String errorTypeLabel,

        @Schema(description = "원문에서의 시작 위치", example = "12")
        Integer startIndex,

        @Schema(description = "원문에서의 끝 위치", example = "15")
        Integer endIndex,

        @Schema(description = "원문 표현", example = "놀앗다")
        String originalText,

        @Schema(description = "제안 표현", example = "놀았다")
        String suggestion,

        @Schema(description = "판단 확신도 (0.0 ~ 1.0)", example = "0.93")
        double confidence,

        @Schema(description = "판단 근거", example = "'았'의 받침 표기")
        String reason
) {

    public static ErrorCandidateResponse from(ErrorCandidate candidate) {
        return new ErrorCandidateResponse(
                candidate.getErrorType(),
                candidate.getErrorType().getLabel(),
                candidate.getStartIndex(),
                candidate.getEndIndex(),
                candidate.getOriginalText(),
                candidate.getSuggestion(),
                candidate.getConfidence(),
                candidate.getReason());
    }
}
