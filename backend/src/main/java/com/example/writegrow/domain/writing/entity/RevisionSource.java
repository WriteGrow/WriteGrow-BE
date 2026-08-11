package com.example.writegrow.domain.writing.entity;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "수정 이력의 출처 (INITIAL: 최초 작성, OCR: 손글씨 변환, CHILD_EDIT: 아동 수정)")
public enum RevisionSource {

    INITIAL,
    OCR,
    CHILD_EDIT
}
