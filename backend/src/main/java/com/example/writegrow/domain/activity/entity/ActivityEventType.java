package com.example.writegrow.domain.activity.entity;

/**
 * 기능명세서에서 기록을 요구하는 이벤트 종류.
 */
public enum ActivityEventType {

    /** 글쓰기 완료(제출) 이벤트. FEAT-01-01 */
    WRITING_SUBMITTED,

    /** 최종본 확정 이벤트. FEAT-01-01 */
    WRITING_CONFIRMED,

    /** OCR 변환 요청 이벤트. FEAT-02-01 */
    OCR_REQUESTED,

    /** OCR 변환 성공 이벤트. FEAT-02-01 */
    OCR_COMPLETED,

    /** OCR 변환 실패 이벤트. FEAT-02-01 예외 처리 */
    OCR_FAILED,

    /** 아동이 변환 텍스트를 수정한 이벤트. FEAT-02-01 */
    OCR_TEXT_EDITED
}
