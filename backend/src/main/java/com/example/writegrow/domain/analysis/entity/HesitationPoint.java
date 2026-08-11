package com.example.writegrow.domain.analysis.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 아동이 머뭇거린 지점. 결과만으로는 알 수 없는 "쓰는 과정"의 흔적이다.
 *
 * @param charIndex  글 전체에서의 글자 위치
 * @param character  해당 글자
 * @param jamo       특히 어려워한 자모
 * @param durationMs 그 글자를 쓰는 데 걸린 시간
 * @param retryCount 고쳐 쓴 횟수
 */
@Schema(description = "머뭇거림 지점")
public record HesitationPoint(

        @Schema(description = "글자 위치", example = "12")
        Integer charIndex,

        @Schema(description = "글자", example = "놀")
        String character,

        @Schema(description = "어려워한 자모", example = "ㄴ")
        String jamo,

        @Schema(description = "해당 글자를 쓰는 데 걸린 시간(ms)", example = "5200")
        Long durationMs,

        @Schema(description = "고쳐 쓴 횟수", example = "2")
        Integer retryCount
) {
}
