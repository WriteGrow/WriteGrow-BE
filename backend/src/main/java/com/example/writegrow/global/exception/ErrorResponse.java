package com.example.writegrow.global.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * 오류 응답 공통 형식. 모든 4xx/5xx 응답이 이 형태다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "오류 응답")
public record ErrorResponse(

        @Schema(description = "요청 성공 여부. 항상 false 다.", example = "false")
        boolean success,

        @Schema(description = "오류 정보")
        ErrorDetail error
) {

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(false, new ErrorDetail(errorCode.getCode(), errorCode.getMessage(), null));
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldErrorDetail> fieldErrors) {
        return new ErrorResponse(false, new ErrorDetail(errorCode.getCode(), errorCode.getMessage(), fieldErrors));
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "오류 상세")
    public record ErrorDetail(

            @Schema(description = "오류 코드. 클라이언트 분기 처리에 사용한다.", example = "EMPTY_CONTENT")
            String code,

            @Schema(description = "사용자에게 보여줄 메시지", example = "빈 글은 제출할 수 없어요.")
            String message,

            @Schema(description = "요청 값 검증 실패 상세. 검증 오류일 때만 채워진다.")
            List<FieldErrorDetail> fieldErrors
    ) {
    }

    @Schema(description = "필드 단위 검증 오류")
    public record FieldErrorDetail(

            @Schema(description = "필드명", example = "content")
            String field,

            @Schema(description = "거절된 값", example = "")
            String rejectedValue,

            @Schema(description = "사유", example = "글 내용은 비어 있을 수 없습니다.")
            String reason
    ) {
    }
}
