package com.example.writegrow.domain.handwriting.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 획을 이루는 점 하나.
 *
 * @param t 글쓰기 세션 시작 시점 기준 경과 시간(ms). "언제 느려졌는가"를 계산하는 근거다.
 */
@Schema(description = "획 위의 한 점")
public record StrokePoint(

        @Schema(description = "캔버스 x 좌표", example = "132.5")
        double x,

        @Schema(description = "캔버스 y 좌표", example = "88.0")
        double y,

        @Schema(description = "세션 시작 기준 경과 시간(ms)", example = "1240")
        long t,

        @Schema(description = "필압 (0.0 ~ 1.0). 기기가 지원하지 않으면 null.", example = "0.42")
        Double pressure
) {
}
