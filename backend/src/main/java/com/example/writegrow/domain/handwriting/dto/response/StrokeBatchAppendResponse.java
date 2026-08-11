package com.example.writegrow.domain.handwriting.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "획 데이터 전송 응답")
public record StrokeBatchAppendResponse(

        @Schema(description = "글 ID", example = "1")
        Long writingId,

        @Schema(description = "저장된 배치 순번", example = "0")
        int batchSeq,

        @Schema(description = "이번 배치의 획 개수", example = "12")
        int strokeCount,

        @Schema(description = "지금까지 저장된 배치 수", example = "3")
        long totalBatches,

        @Schema(description = "이미 받은 배치라 새로 저장하지 않았는지 여부", example = "false")
        boolean duplicated
) {
}
