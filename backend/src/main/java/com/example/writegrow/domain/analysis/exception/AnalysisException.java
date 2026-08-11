package com.example.writegrow.domain.analysis.exception;

import com.example.writegrow.global.exception.BaseException;

public class AnalysisException extends BaseException {

    public AnalysisException(AnalysisErrorCode errorCode) {
        super(errorCode);
    }
}
