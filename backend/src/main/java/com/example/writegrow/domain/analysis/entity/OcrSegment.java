package com.example.writegrow.domain.analysis.entity;

import com.example.writegrow.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
 * 구절 단위 OCR 결과.
 *
 * <p>{@code lowConfidence} 가 true 인 구절은 아동에게 오류로 확정해 보여주지 않는다.
 * (기능명세서 FEAT-02-01 비즈니스 규칙: 낮은 OCR 신뢰도 결과는 아동 교정 대상에 포함하지 않음)
 */
@Getter
@Entity
@Table(name = "ocr_segment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OcrSegment extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ocr_result_id", nullable = false)
    private OcrResult ocrResult;

    @Column(name = "seq", nullable = false)
    private int seq;

    @Column(name = "segment_text", nullable = false, columnDefinition = "TEXT")
    private String text;

    @Column(name = "confidence", nullable = false)
    private double confidence;

    @Column(name = "start_index")
    private Integer startIndex;

    @Column(name = "end_index")
    private Integer endIndex;

    @Column(name = "low_confidence", nullable = false)
    private boolean lowConfidence;

    OcrSegment(OcrResult ocrResult, int seq, String text, double confidence,
               Integer startIndex, Integer endIndex, boolean lowConfidence) {
        this.ocrResult = ocrResult;
        this.seq = seq;
        this.text = text;
        this.confidence = confidence;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.lowConfidence = lowConfidence;
    }
}
