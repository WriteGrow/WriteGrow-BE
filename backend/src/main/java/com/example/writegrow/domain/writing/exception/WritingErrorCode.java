package com.example.writegrow.domain.writing.exception;

import com.example.writegrow.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum WritingErrorCode implements ErrorCode {

    WRITING_NOT_FOUND(HttpStatus.NOT_FOUND, "글을 찾을 수 없어요."),
    EMPTY_CONTENT(HttpStatus.BAD_REQUEST, "빈 글은 제출할 수 없어요."),
    FORBIDDEN_PROFILE(HttpStatus.FORBIDDEN, "내가 쓴 글만 볼 수 있어요."),
    ALREADY_SUBMITTED(HttpStatus.CONFLICT, "이미 제출한 글이에요."),
    INVALID_INPUT_TYPE(HttpStatus.BAD_REQUEST, "입력 방식에 맞지 않는 요청이에요."),
    NOT_READY_FOR_CONFIRM(HttpStatus.CONFLICT, "글자 변환이 끝난 뒤에 확인할 수 있어요."),
    ALREADY_CONFIRMED(HttpStatus.CONFLICT, "이미 확인을 마친 글이에요."),
    NOT_REWRITABLE(HttpStatus.CONFLICT, "글자 변환이 끝난 뒤에 다시 쓸 수 있어요."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
