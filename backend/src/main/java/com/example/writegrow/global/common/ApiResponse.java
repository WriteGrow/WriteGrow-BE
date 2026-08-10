package com.example.writegrow.global.common;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 성공 응답 공통 래퍼.
 *
 * <p>오류 응답은 {@link com.example.writegrow.global.exception.ErrorResponse} 를 사용한다.
 * 두 타입을 나눠 두어야 Swagger 문서에서 성공 예시에 오류 필드가, 오류 예시에 데이터 필드가 섞이지 않는다.
 */
@Schema(description = "성공 응답")
public record ApiResponse<T>(

        @Schema(description = "요청 성공 여부", example = "true")
        boolean success,

        @Schema(description = "응답 본문")
        T data
) {

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data);
    }

    public static ApiResponse<Void> ok() {
        return new ApiResponse<>(true, null);
    }
}
