package com.example.writegrow.domain.writing.entity;

import com.example.writegrow.domain.writing.exception.WritingErrorCode;
import com.example.writegrow.domain.writing.exception.WritingException;
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
import java.util.Objects;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아동이 작성한 글 한 편. 상태 전이 규칙은 {@link WritingStatus} 참고.
 *
 * <p>다른 애그리거트인 프로필은 ID 로만 참조한다.
 */
@Getter
@Entity
@Table(name = "writing")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Writing extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type", nullable = false, length = 20)
    private InputType inputType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private WritingStatus status;

    @Column(name = "topic", length = 100)
    private String topic;

    @Column(name = "original_text", columnDefinition = "TEXT")
    private String originalText;

    @Column(name = "final_text", columnDefinition = "TEXT")
    private String finalText;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @OrderBy("revisionNo ASC")
    @OneToMany(mappedBy = "writing", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WritingRevision> revisions = new ArrayList<>();

    private Writing(Long profileId, InputType inputType, String topic) {
        this.profileId = profileId;
        this.inputType = inputType;
        this.topic = topic;
        this.status = WritingStatus.DRAFT;
    }

    public static Writing create(Long profileId, InputType inputType, String topic) {
        return new Writing(profileId, inputType, topic);
    }

    public List<WritingRevision> getRevisions() {
        return Collections.unmodifiableList(revisions);
    }

    /**
     * 작성 중인 키보드 글의 임시 저장.
     */
    public void updateDraftContent(String content) {
        requireStatus(WritingStatus.DRAFT, WritingErrorCode.ALREADY_SUBMITTED);
        requireInputType(InputType.KEYBOARD);
        this.originalText = content;
    }

    /**
     * 키보드 글 제출. 분석 대상이 아니므로 즉시 최종본으로 확정된다.
     */
    public void submitKeyboard(String content) {
        requireStatus(WritingStatus.DRAFT, WritingErrorCode.ALREADY_SUBMITTED);
        requireInputType(InputType.KEYBOARD);
        String trimmed = requireNotBlank(content);

        this.originalText = trimmed;
        this.finalText = trimmed;
        this.submittedAt = LocalDateTime.now();
        this.status = WritingStatus.CONFIRMED;
        addRevision(trimmed, RevisionSource.INITIAL);
    }

    /**
     * 손글씨 글 제출. 이후 AI 분석 파이프라인이 비동기로 이어진다.
     */
    public void submitHandwriting() {
        requireStatus(WritingStatus.DRAFT, WritingErrorCode.ALREADY_SUBMITTED);
        requireInputType(InputType.PEN);

        this.submittedAt = LocalDateTime.now();
        this.status = WritingStatus.SUBMITTED;
    }

    /**
     * OCR 변환 결과를 원문으로 반영한다. 아동 확인 전까지는 최종본으로 확정하지 않는다.
     */
    public void applyOcrText(String ocrText) {
        if (status != WritingStatus.SUBMITTED && status != WritingStatus.ANALYSIS_FAILED) {
            throw new WritingException(WritingErrorCode.ALREADY_CONFIRMED);
        }
        String trimmed = requireNotBlank(ocrText);

        this.originalText = trimmed;
        this.status = WritingStatus.ANALYZED;
        addRevision(trimmed, RevisionSource.OCR);
    }

    /**
     * 분석 실패. 손글씨 원본은 그대로 보존되며 재시도할 수 있다.
     */
    public void markAnalysisFailed() {
        this.status = WritingStatus.ANALYSIS_FAILED;
    }

    /**
     * 재분석 시작. {@link WritingStatus#ANALYSIS_FAILED} 상태에서만 호출된다.
     */
    public void markAnalysisRetried() {
        this.status = WritingStatus.SUBMITTED;
    }

    /**
     * 아동이 변환 텍스트를 확인/수정하고 최종본을 확정한다.
     *
     * @return 아동이 실제로 내용을 수정했으면 true
     */
    public boolean confirmText(String content) {
        if (status == WritingStatus.CONFIRMED) {
            throw new WritingException(WritingErrorCode.ALREADY_CONFIRMED);
        }
        if (status != WritingStatus.ANALYZED) {
            throw new WritingException(WritingErrorCode.NOT_READY_FOR_CONFIRM);
        }
        String trimmed = requireNotBlank(content);

        boolean edited = !Objects.equals(trimmed, originalText);
        this.finalText = trimmed;
        this.status = WritingStatus.CONFIRMED;
        if (edited) {
            addRevision(trimmed, RevisionSource.CHILD_EDIT);
        }
        return edited;
    }

    /**
     * 본인 글인지 확인한다. 인증이 도입되기 전이지만 소유권 규칙은 지금부터 지킨다.
     */
    public void validateOwner(Long profileId) {
        if (!Objects.equals(this.profileId, profileId)) {
            throw new WritingException(WritingErrorCode.FORBIDDEN_PROFILE);
        }
    }

    public boolean isHandwriting() {
        return inputType == InputType.PEN;
    }

    private void addRevision(String content, RevisionSource source) {
        revisions.add(new WritingRevision(this, revisions.size() + 1, content, source));
    }

    private void requireStatus(WritingStatus expected, WritingErrorCode errorCode) {
        if (status != expected) {
            throw new WritingException(errorCode);
        }
    }

    private void requireInputType(InputType expected) {
        if (inputType != expected) {
            throw new WritingException(WritingErrorCode.INVALID_INPUT_TYPE);
        }
    }

    private static String requireNotBlank(String content) {
        if (content == null || content.isBlank()) {
            throw new WritingException(WritingErrorCode.EMPTY_CONTENT);
        }
        return content.trim();
    }
}
