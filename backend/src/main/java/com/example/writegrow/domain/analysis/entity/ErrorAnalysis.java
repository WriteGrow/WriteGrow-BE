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

/**
 * 확정된 글 한 편에 대한 오류 분석 결과. (기능명세서 REQ-03 기능 1)
 *
 * <p>OCR 분석({@link OcrResult})과는 별개의 두 번째 분석이다. OCR 은 "무엇이라고 썼는가"를,
 * 여기는 "확정된 문장에 어떤 오류 후보가 있는가"를 다룬다. 키보드 글에는 OCR 이 없지만
 * 오류 분석은 동일하게 수행된다.
 *
 * <p>실패 상태를 남기는 것은 명세의 예외 규칙이다 — "분석 실패 상태는 이후 재처리할 수 있도록
 * 기록한다".
 */
@Getter
@Entity
@Table(name = "error_analysis")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorAnalysis extends BaseTimeEntity {

    private static final int MAX_FAILURE_REASON_LENGTH = 500;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "writing_id", nullable = false, unique = true)
    private Long writingId;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AnalysisStatus status;

    @Column(name = "analyzed_text", columnDefinition = "TEXT")
    private String analyzedText;

    @Column(name = "provider", length = 50)
    private String provider;

    @Column(name = "requested_at")
    private LocalDateTime requestedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failure_reason", length = MAX_FAILURE_REASON_LENGTH)
    private String failureReason;

    @OrderBy("seq ASC")
    @OneToMany(mappedBy = "errorAnalysis", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ErrorCandidate> candidates = new ArrayList<>();

    private ErrorAnalysis(Long writingId, Long profileId) {
        this.writingId = writingId;
        this.profileId = profileId;
        this.status = AnalysisStatus.PENDING;
    }

    public static ErrorAnalysis create(Long writingId, Long profileId) {
        return new ErrorAnalysis(writingId, profileId);
    }

    public List<ErrorCandidate> getCandidates() {
        return Collections.unmodifiableList(candidates);
    }

    /**
     * 분석 시작. 재분석이면 이전 후보를 비운다.
     */
    public void markProcessing(String analyzedText) {
        this.status = AnalysisStatus.PROCESSING;
        this.analyzedText = analyzedText;
        this.requestedAt = LocalDateTime.now();
        this.completedAt = null;
        this.failureReason = null;
        this.candidates.clear();
    }

    public void markSucceeded(String provider) {
        this.status = AnalysisStatus.SUCCEEDED;
        this.provider = provider;
        this.completedAt = LocalDateTime.now();
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        this.status = AnalysisStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.failureReason = truncate(reason);
    }

    public void addCandidate(int seq, ErrorType errorType, Integer startIndex, Integer endIndex,
                             String originalText, String suggestion, double confidence,
                             String reason, boolean lowConfidence) {
        this.candidates.add(new ErrorCandidate(this, seq, errorType, startIndex, endIndex,
                originalText, suggestion, confidence, reason, lowConfidence));
    }

    public boolean isFailed() {
        return status == AnalysisStatus.FAILED;
    }

    /**
     * 아동에게 전달할 확정 오류. 낮은 확신도 후보는 제외한다.
     * (명세 표시 규칙: "교정 대상으로 확정된 높은 확신도 오류만 전달한다")
     */
    public List<ErrorCandidate> confirmedCandidates() {
        return candidates.stream().filter(candidate -> !candidate.isLowConfidence()).toList();
    }

    /**
     * 보호자·교사가 검토할 낮은 확신도 후보.
     */
    public List<ErrorCandidate> reviewCandidates() {
        return candidates.stream().filter(ErrorCandidate::isLowConfidence).toList();
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
