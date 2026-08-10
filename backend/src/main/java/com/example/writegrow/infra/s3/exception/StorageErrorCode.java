package com.example.writegrow.infra.s3.exception;

import com.example.writegrow.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum StorageErrorCode implements ErrorCode {

    UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일을 저장하지 못했어요. 잠시 후 다시 시도해 주세요."),
    PRESIGN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 주소를 만들지 못했어요."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
