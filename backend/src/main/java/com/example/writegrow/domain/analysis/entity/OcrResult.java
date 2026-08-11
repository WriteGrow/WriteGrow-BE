package com.example.writegrow.domain.analysis.entity;

import com.example.writegrow.global.common.BaseTimeEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "ocr_result")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OcrResult extends BaseTimeEntity {

    private static final int MAX_FAILURE_REASON_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "writing_id", nullable = false, unique = true)
    private Long writingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AnalysisStatus status;

    @Column(name = "full_text", columnDefinition = "TEXT")
    private String fullText;

    @Column(name = "overall_confidence")
    private Double overallConfidence;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failure_reason", length = MAX_FAILURE_REASON_LENGTH)
    private String failureReason;

    @OrderBy("seq ASC")
    @OneToMany(mappedBy = "ocrResult", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OcrSegment> segments = new ArrayList<>();

    private OcrResult(Long writingId) {
        this.writingId = writingId;
        this.status = AnalysisStatus.PENDING;
    }

    public static OcrResult create(Long writingId) {
        return new OcrResult(writingId);
    }

    public List<OcrSegment> getSegments() {
        return Collections.unmodifiableList(segments);
    }

    /**
     * 분석 시작. 재시도 시 이전 구절 결과를 비운다.
     */
    public void markProcessing() {
        this.status = AnalysisStatus.PROCESSING;
        this.requestedAt = LocalDateTime.now();
        this.completedAt = null;
        this.failureReason = null;
        this.segments.clear();
    }

    public void markSucceeded(String fullText, Double overallConfidence, String provider) {
        this.status = AnalysisStatus.SUCCEEDED;
        this.fullText = fullText;
        this.overallConfidence = overallConfidence;
        this.provider = provider;
        this.completedAt = LocalDateTime.now();
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        this.status = AnalysisStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.failureReason = truncate(reason);
    }

    public void addSegment(int seq, String text, double confidence,
                           Integer startIndex, Integer endIndex, boolean lowConfidence) {
        this.segments.add(new OcrSegment(this, seq, text, confidence, startIndex, endIndex, lowConfidence));
    }

    public boolean isFailed() {
        return status == AnalysisStatus.FAILED;
    }

    private static String truncate(String reason) {
        if (reason == null) {
            return null;
        }
        return reason.length() <= MAX_FAILURE_REASON_LENGTH
                ? reason
                : reason.substring(0, MAX_FAILURE_REASON_LENGTH);
    }
}
