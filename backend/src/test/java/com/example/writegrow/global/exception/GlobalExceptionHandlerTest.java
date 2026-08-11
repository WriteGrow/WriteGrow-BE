package com.example.writegrow.global.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.writegrow.domain.account.exception.AccountErrorCode;
import com.example.writegrow.domain.account.exception.AccountException;
import com.example.writegrow.domain.analysis.exception.AnalysisErrorCode;
import com.example.writegrow.domain.analysis.exception.AnalysisException;
import com.example.writegrow.domain.handwriting.exception.HandwritingErrorCode;
import com.example.writegrow.domain.handwriting.exception.HandwritingException;
import com.example.writegrow.domain.writing.exception.WritingErrorCode;
import com.example.writegrow.domain.writing.exception.WritingException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@DisplayName("GlobalExceptionHandler 단위 테스트")
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    static Stream<Arguments> domainExceptions() {
        return Stream.of(
                Arguments.of(new WritingException(WritingErrorCode.EMPTY_CONTENT),
                        HttpStatus.BAD_REQUEST, "EMPTY_CONTENT"),
                Arguments.of(new WritingException(WritingErrorCode.FORBIDDEN_PROFILE),
                        HttpStatus.FORBIDDEN, "FORBIDDEN_PROFILE"),
                Arguments.of(new WritingException(WritingErrorCode.WRITING_NOT_FOUND),
                        HttpStatus.NOT_FOUND, "WRITING_NOT_FOUND"),
                Arguments.of(new WritingException(WritingErrorCode.NOT_READY_FOR_CONFIRM),
                        HttpStatus.CONFLICT, "NOT_READY_FOR_CONFIRM"),
                Arguments.of(new AccountException(AccountErrorCode.CONSENT_REQUIRED),
                        HttpStatus.FORBIDDEN, "CONSENT_REQUIRED"),
                Arguments.of(new HandwritingException(HandwritingErrorCode.NOT_HANDWRITING),
                        HttpStatus.BAD_REQUEST, "NOT_HANDWRITING"),
                Arguments.of(new AnalysisException(AnalysisErrorCode.NOT_FAILED_STATE),
                        HttpStatus.CONFLICT, "NOT_FAILED_STATE"));
    }

    @ParameterizedTest(name = "{2} → {1}")
    @MethodSource("domainExceptions")
    @DisplayName("도메인 예외는 에러코드가 정한 상태와 코드로 변환된다")
    void convertsDomainException(BaseException exception, HttpStatus expectedStatus, String expectedCode) {
        ResponseEntity<ErrorResponse> response = handler.handleBaseException(exception);

        assertThat(response.getStatusCode()).isEqualTo(expectedStatus);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().code()).isEqualTo(expectedCode);
        assertThat(response.getBody().error().message()).isNotBlank();
        assertThat(response.getBody().error().fieldErrors()).isNull();
    }

    @Test
    @DisplayName("예상하지 못한 예외는 500 과 공통 메시지로 변환된다")
    void convertsUnexpectedException() {
        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(new IllegalStateException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_ERROR");
        // 내부 예외 메시지가 사용자에게 그대로 노출되지 않아야 한다.
        assertThat(response.getBody().error().message()).doesNotContain("boom");
    }
}
