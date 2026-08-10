package com.example.writegrow.global.exception;

/**
 * 특정 도메인에 속하지 않는 공통 예외.
 */
public class GlobalException extends BaseException {

    public GlobalException(ErrorCode errorCode) {
        super(errorCode);
    }

    public GlobalException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
