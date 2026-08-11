package com.example.writegrow.domain.writing.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "입력 방식 (KEYBOARD: 키보드, PEN: 태블릿 펜 손글씨)")
public enum InputType {

    KEYBOARD,
    PEN
}
