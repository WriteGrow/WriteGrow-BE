package com.example.writegrow.domain.handwriting.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "손글씨 이미지 업로드 응답")
public record HandwritingImageUploadResponse(

        @Schema(description = "글 ID", example = "1")
        Long writingId,

        @Schema(description = "업로드된 이미지의 열람용 URL(만료 시간이 있는 임시 주소)")
        String imageUrl
) {
}
