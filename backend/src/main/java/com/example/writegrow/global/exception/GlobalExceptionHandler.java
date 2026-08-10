package com.example.writegrow.global.exception;

import com.example.writegrow.global.exception.ErrorResponse.FieldErrorDetail;
import java.util.List;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 도메인 예외와 프레임워크 예외를 {@link ErrorResponse} 형태로 변환하는 단일 지점.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        log.warn("도메인 예외 발생: code={}, message={}", errorCode.getCode(), exception.getMessage());
        return toResponse(errorCode);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException exception) {
        List<FieldErrorDetail> fieldErrors = exception.getBindingResult().getFieldErrors().stream()
                .map(GlobalExceptionHandler::toFieldErrorDetail)
                .toList();
        return ResponseEntity.status(GlobalErrorCode.INVALID_REQUEST.getStatus())
                .body(ErrorResponse.of(GlobalErrorCode.INVALID_REQUEST, fieldErrors));
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleHandlerMethodValidation(HandlerMethodValidationException exception) {
        log.warn("요청 파라미터 검증 실패: {}", exception.getMessage());
        return toResponse(GlobalErrorCode.INVALID_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        FieldErrorDetail fieldError = new FieldErrorDetail(
                exception.getName(),
                Objects.toString(exception.getValue(), null),
                "허용되지 않는 값입니다.");
        return ResponseEntity.status(GlobalErrorCode.INVALID_REQUEST.getStatus())
                .body(ErrorResponse.of(GlobalErrorCode.INVALID_REQUEST, List.of(fieldError)));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException exception) {
        log.warn("필수 헤더 누락: {}", exception.getHeaderName());
        return toResponse(GlobalErrorCode.MISSING_PROFILE_HEADER);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException exception) {
        log.warn("요청 본문을 읽을 수 없습니다: {}", exception.getMessage());
        return toResponse(GlobalErrorCode.INVALID_REQUEST);
    }

    /**
     * 존재하지 않는 필드로 정렬을 요청한 경우(예: {@code ?sort=nope,desc}). 500 이 아니라 400 으로 돌려준다.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidSort(PropertyReferenceException exception) {
        log.warn("잘못된 정렬 기준: {}", exception.getPropertyName());
        FieldErrorDetail fieldError = new FieldErrorDetail(
                "sort", exception.getPropertyName(), "정렬할 수 없는 필드입니다.");
        return ResponseEntity.status(GlobalErrorCode.INVALID_REQUEST.getStatus())
                .body(ErrorResponse.of(GlobalErrorCode.INVALID_REQUEST, List.of(fieldError)));
    }

    /**
     * 존재하지 않는 경로 요청. 아래 {@code Exception} 핸들러가 500 으로 감싸지 않도록 먼저 처리한다.
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException exception) {
        return toResponse(GlobalErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("처리되지 않은 예외", exception);
        return toResponse(GlobalErrorCode.INTERNAL_ERROR);
    }

    private static ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode) {
        return ResponseEntity.status(errorCode.getStatus()).body(ErrorResponse.of(errorCode));
    }

    private static FieldErrorDetail toFieldErrorDetail(FieldError fieldError) {
        return new FieldErrorDetail(
                fieldError.getField(),
                Objects.toString(fieldError.getRejectedValue(), null),
                fieldError.getDefaultMessage());
    }
}
