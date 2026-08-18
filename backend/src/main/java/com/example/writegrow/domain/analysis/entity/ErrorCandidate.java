package com.example.writegrow.domain.analysis.entity;

import com.example.writegrow.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 오류 후보 하나.
 *
 * <p>{@code lowConfidence} 가 true 면 <b>아동에게 노출하지 않고</b> 보호자·교사 검토 대상으로만
 * 분리하며, 반복 오류 프로필 집계에도 반영하지 않는다. (명세 비즈니스 규칙 4)
 *
 * <p>분석 시각은 별도 컬럼을 두지 않고 {@link ErrorAnalysis#getCompletedAt()} 을 쓴다.
 * 한 번의 분석에서 나온 후보는 모두 같은 시각이라 중복 저장할 이유가 없다.
 */
@Getter
@Entity
@Table(name = "error_candidate")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorCandidate extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "error_analysis_id", nullable = false)
    private ErrorAnalysis errorAnalysis;

    @Column(name = "seq", nullable = false)
    private int seq;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_type", nullable = false, length = 30)
    private ErrorType errorType;

    @Column(name = "start_index")
    private Integer startIndex;

    @Column(name = "end_index")
    private Integer endIndex;

    @Column(name = "original_text", columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "suggestion", columnDefinition = "TEXT")
    private String suggestion;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    /** 판단 근거. 낮은 확신도 후보를 보호자가 검토할 때 판단 재료가 된다. */
    @Column(name = "reason", length = 300)
    private String reason;

    @Column(name = "low_confidence", nullable = false)
    private boolean lowConfidence;

    ErrorCandidate(ErrorAnalysis errorAnalysis, int seq, ErrorType errorType,
                   Integer startIndex, Integer endIndex, String originalText, String suggestion,
                   double confidence, String reason, boolean lowConfidence) {
        this.errorAnalysis = errorAnalysis;
        this.seq = seq;
        this.errorType = errorType;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.originalText = originalText;
        this.suggestion = suggestion;
        this.confidence = confidence;
        this.reason = reason;
        this.lowConfidence = lowConfidence;
    }
}
