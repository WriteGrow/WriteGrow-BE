package com.example.writegrow.domain.handwriting.dto.request;

import com.example.writegrow.domain.handwriting.entity.StrokeData;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

/**
 * 글을 쓰는 도중 주기적으로 전송하는 획 묶음.
 *
 * <p>같은 {@code batchSeq} 로 다시 보내도 중복 저장되지 않으므로 네트워크가 끊겼을 때 그대로 재전송하면 된다.
 */
@Schema(description = "획 데이터 전송 요청")
public record StrokeBatchAppendRequest(

        @Schema(description = "배치 순번(0부터 증가). 재전송 시 같은 값을 사용한다.", example = "0")
        @PositiveOrZero(message = "배치 순번은 0 이상이어야 합니다.")
        int batchSeq,

        @Schema(description = "이번 배치에 담긴 획 목록")
        @NotEmpty(message = "획 데이터는 비어 있을 수 없습니다.")
        List<StrokeData> strokes
) {
}
