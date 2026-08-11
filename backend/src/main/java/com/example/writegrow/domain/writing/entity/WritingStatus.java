package com.example.writegrow.domain.writing.entity;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 글의 상태 전이.
 *
 * <pre>
 * 키보드: DRAFT ─submit─▶ CONFIRMED
 * 펜:     DRAFT ─submit─▶ SUBMITTED ─분석 성공─▶ ANALYZED ─아동 확인─▶ CONFIRMED
 *                             └────분석 실패────▶ ANALYSIS_FAILED ─재시도─▶ SUBMITTED
 * </pre>
 */
@Schema(description = "글 상태")
public enum WritingStatus {

    /** 작성 중. */
    DRAFT,

    /** 손글씨 제출 완료, AI 분석 진행 중. */
    SUBMITTED,

    /** OCR 변환이 끝나 아동 확인을 기다리는 상태. */
    ANALYZED,

    /** 최종본이 확정된 상태. */
    CONFIRMED,

    /** 분석에 실패한 상태. 손글씨 원본은 보존되며 재시도할 수 있다. */
    ANALYSIS_FAILED
}
