package com.example.writegrow.infra.ai.exception;

import com.example.writegrow.global.exception.BaseException;

public class AiAnalysisException extends BaseException {

    public AiAnalysisException(AiAnalysisErrorCode errorCode) {
        super(errorCode);
    }

    public AiAnalysisException(AiAnalysisErrorCode errorCode, Throwable cause) {
        super(errorCode, cause);
    }
}
