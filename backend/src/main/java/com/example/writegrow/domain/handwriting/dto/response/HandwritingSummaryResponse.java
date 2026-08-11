package com.example.writegrow.domain.handwriting.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "손글씨 원본 정보와 과정 요약 지표")
public record HandwritingSummaryResponse(

        @Schema(description = "손글씨 원본 이미지 열람 URL")
        String imageUrl,

        @Schema(description = "획 데이터(JSON) 열람 URL")
        String strokeDataUrl,

        @Schema(description = "총 획 개수", example = "48")
        Integer strokeCount,

        @Schema(description = "글을 쓰는 데 걸린 전체 시간(ms)", example = "92000")
        Long totalDurationMs,

        @Schema(description = "캔버스 너비(px)", example = "1024")
        Integer canvasWidth,

        @Schema(description = "캔버스 높이(px)", example = "768")
        Integer canvasHeight
) {
}
