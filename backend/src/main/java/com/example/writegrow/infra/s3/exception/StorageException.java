package com.example.writegrow.infra.s3.exception;

import com.example.writegrow.global.exception.BaseException;

public class StorageException extends BaseException {

    public StorageException(StorageErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
