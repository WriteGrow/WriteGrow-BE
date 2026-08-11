package com.example.writegrow.domain.analysis.exception;

import com.example.writegrow.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AnalysisErrorCode implements ErrorCode {

    ANALYSIS_NOT_FOUND(HttpStatus.NOT_FOUND, "아직 분석 결과가 없어요."),
    NOT_FAILED_STATE(HttpStatus.CONFLICT, "실패한 분석만 다시 시도할 수 있어요."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
