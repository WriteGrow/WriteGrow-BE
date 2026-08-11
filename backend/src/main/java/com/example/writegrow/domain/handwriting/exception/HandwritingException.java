package com.example.writegrow.domain.handwriting.exception;

import com.example.writegrow.global.exception.BaseException;

public class HandwritingException extends BaseException {

    public HandwritingException(HandwritingErrorCode errorCode) {
        super(errorCode);
    }

    public HandwritingException(HandwritingErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
