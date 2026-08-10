package com.example.writegrow.infra.ai.exception;

import com.example.writegrow.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AiAnalysisErrorCode implements ErrorCode {

    AI_CALL_FAILED(HttpStatus.BAD_GATEWAY, "글자 변환 서버에 연결하지 못했어요."),
    AI_EMPTY_RESPONSE(HttpStatus.BAD_GATEWAY, "글자를 알아보지 못했어요."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
