package com.example.writegrow.domain.handwriting.exception;

import com.example.writegrow.global.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum HandwritingErrorCode implements ErrorCode {

    NOT_HANDWRITING(HttpStatus.BAD_REQUEST, "손글씨로 쓴 글에만 사용할 수 있어요."),
    EMPTY_IMAGE(HttpStatus.BAD_REQUEST, "손글씨 이미지가 비어 있어요."),
    UNSUPPORTED_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "PNG 또는 JPEG 이미지만 올릴 수 있어요."),
    IMAGE_READ_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "이미지를 읽지 못했어요. 다시 시도해 주세요."),
    EMPTY_STROKE_BATCH(HttpStatus.BAD_REQUEST, "보낼 획 데이터가 없어요."),
    IMAGE_REQUIRED(HttpStatus.CONFLICT, "손글씨 이미지를 먼저 올려 주세요."),
    STROKE_DATA_REQUIRED(HttpStatus.CONFLICT, "글을 쓴 과정(획) 데이터가 없어요."),
    ASSET_NOT_FOUND(HttpStatus.NOT_FOUND, "손글씨 원본을 찾을 수 없어요."),
    STROKE_SERIALIZATION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "획 데이터를 저장하지 못했어요."),
    ;

    private final HttpStatus status;
    private final String message;

    @Override
    public String getCode() {
        return name();
    }
}
