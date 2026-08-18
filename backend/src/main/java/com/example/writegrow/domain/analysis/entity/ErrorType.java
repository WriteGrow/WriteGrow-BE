package com.example.writegrow.domain.analysis.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 오류 후보의 유형. 기능명세서 REQ-03 이 정한 여섯 가지를 그대로 쓴다.
 *
 * <p>{@code label} 은 아동·보호자 화면에 그대로 노출되는 표기다. 프론트가 유형별 문구를 따로
 * 관리하면 명세와 어긋날 수 있어 서버가 함께 내려준다.
 */
@Getter
@RequiredArgsConstructor
@Schema(description = "오류 유형")
public enum ErrorType {

    SPELLING("맞춤법"),
    SPACING("띄어쓰기"),
    FINAL_CONSONANT("받침"),
    PARTICLE_ENDING("조사·어미"),
    SENTENCE_STRUCTURE("문장 구성"),
    VOCABULARY("어휘 표현");

    private final String label;

    /**
     * AI 응답의 문자열을 유형으로 바꾼다. 모르는 값이면 비어 있는 결과를 돌려준다.
     *
     * <p>AI 팀이 유형을 추가해도 백엔드가 깨지지 않아야 한다. enum 으로 직접 역직렬화하면
     * 새 유형 하나에 분석 전체가 실패한다.
     */
    public static Optional<ErrorType> from(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(type -> type.name().equalsIgnoreCase(raw.trim()))
                .findFirst();
    }
}
