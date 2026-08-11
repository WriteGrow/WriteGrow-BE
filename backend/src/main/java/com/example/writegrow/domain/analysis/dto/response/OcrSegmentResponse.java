package com.example.writegrow.domain.analysis.dto.response;

import com.example.writegrow.domain.analysis.entity.OcrSegment;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "구절 단위 변환 결과")
public record OcrSegmentResponse(

        @Schema(description = "구절 순번", example = "2")
        int seq,

        @Schema(description = "변환된 구절", example = "놀앗다")
        String text,

        @Schema(description = "판단 확신도 (0.0 ~ 1.0)", example = "0.62")
        double confidence,

        @Schema(description = "원문에서의 시작 위치", example = "12")
        Integer startIndex,

        @Schema(description = "원문에서의 끝 위치", example = "15")
        Integer endIndex,

        @Schema(description = "확신도가 낮아 아동 교정 대상에서 제외된 구절인지 여부. true 면 오류로 확정하지 않는다.",
                example = "true")
        boolean lowConfidence
) {

    public static OcrSegmentResponse from(OcrSegment segment) {
        return new OcrSegmentResponse(
                segment.getSeq(),
                segment.getText(),
                segment.getConfidence(),
                segment.getStartIndex(),
                segment.getEndIndex(),
                segment.isLowConfidence());
    }
}
