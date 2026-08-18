package com.example.writegrow.domain.analysis.dto.response;

import com.example.writegrow.domain.analysis.entity.ErrorProfile;
import com.example.writegrow.domain.analysis.entity.ErrorType;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * 아동별 누적 반복 오류 프로필. 확정 오류만 집계된 값이다.
 */
@Schema(description = "아동의 반복 오류 프로필")
public record ErrorProfileResponse(

        @Schema(description = "아동 프로필 ID", example = "1")
        Long profileId,

        @Schema(description = "누적 발생이 잦은 순으로 정렬된 유형별 지표")
        List<Item> items
) {

    @Schema(description = "오류 유형별 누적 지표")
    public record Item(

            @Schema(description = "오류 유형", example = "SPACING")
            ErrorType errorType,

            @Schema(description = "화면에 보여줄 유형 이름", example = "띄어쓰기")
            String errorTypeLabel,

            @Schema(description = "누적 발생 횟수", example = "12")
            int occurrenceCount,

            @Schema(description = "스스로 고쳐낸 횟수", example = "8")
            int correctionSuccessCount,

            @Schema(description = "자기교정 성공률 (0.0 ~ 1.0)", example = "0.67")
            double correctionRate,

            @Schema(description = "최근 발생일", example = "2026-08-18")
            LocalDate lastOccurredOn
    ) {

        public static Item from(ErrorProfile profile) {
            return new Item(
                    profile.getErrorType(),
                    profile.getErrorType().getLabel(),
                    profile.getOccurrenceCount(),
                    profile.getCorrectionSuccessCount(),
                    profile.correctionRate(),
                    profile.getLastOccurredOn());
        }
    }

    public static ErrorProfileResponse of(Long profileId, List<ErrorProfile> profiles) {
        return new ErrorProfileResponse(
                profileId,
                profiles.stream().map(Item::from).toList());
    }
}
