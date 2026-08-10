package com.example.writegrow.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 도메인별 에러코드 enum 이 구현하는 공통 계약.
 * 예) {@code WritingErrorCode implements ErrorCode}
 */
public interface ErrorCode {

    /**
     * 응답 HTTP 상태.
     */
    HttpStatus getStatus();

    /**
     * 클라이언트가 분기 처리에 사용하는 문자열 코드. enum 상수명을 그대로 사용한다.
     */
    String getCode();

    /**
     * 사용자에게 보여줄 메시지. 아동이 읽을 수 있는 표현을 우선한다.
     */
    String getMessage();
}
