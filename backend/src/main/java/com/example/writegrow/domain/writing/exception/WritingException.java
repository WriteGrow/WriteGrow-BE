package com.example.writegrow.domain.writing.exception;

import com.example.writegrow.global.exception.BaseException;

public class WritingException extends BaseException {

    public WritingException(WritingErrorCode errorCode) {
        super(errorCode);
    }
}
