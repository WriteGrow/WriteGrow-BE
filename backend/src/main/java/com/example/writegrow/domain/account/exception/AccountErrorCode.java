package com.example.writegrow.domain.account.exception;

import com.example.writegrow.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AccountErrorCode implements ErrorCode {

    ACCOUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "계정을 찾을 수 없습니다."),
    PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "프로필을 찾을 수 없습니다."),
    NOT_CHILD_PROFILE(HttpStatus.FORBIDDEN, "아동 프로필만 글을 쓸 수 있어요."),
    CONSENT_REQUIRED(HttpStatus.FORBIDDEN, "보호자 동의가 확인되어야 글쓰기를 시작할 수 있어요."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
