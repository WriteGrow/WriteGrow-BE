package com.example.writegrow.global.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * 페이지 조회 결과 공통 응답. Spring Data 의 {@link Page} 를 그대로 노출하지 않기 위한 DTO.
 */
@Schema(description = "페이지 응답")
public record PageResponse<T>(
        @Schema(description = "현재 페이지 내용")
        List<T> content,

        @Schema(description = "현재 페이지 번호(0부터 시작)", example = "0")
        int page,

        @Schema(description = "페이지 크기", example = "20")
        int size,

        @Schema(description = "전체 요소 수", example = "37")
        long totalElements,

        @Schema(description = "전체 페이지 수", example = "2")
        int totalPages,

        @Schema(description = "마지막 페이지 여부", example = "false")
        boolean last
) {

    public static <E, T> PageResponse<T> of(Page<E> page, Function<E, T> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast()
        );
    }
}
