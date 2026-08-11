package com.example.writegrow.domain.handwriting.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 글을 쓰는 도중 주기적으로 올라온 획 묶음. 제출 시 순번대로 병합되어 S3 로 이동한다.
 */
@Getter
@Entity
@Table(name = "handwriting_stroke_batch",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_handwriting_stroke_batch", columnNames = {"writing_id", "batch_seq"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StrokeBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "writing_id", nullable = false)
    private Long writingId;

    @Column(name = "batch_seq", nullable = false)
    private int batchSeq;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false)
    private StrokePayload payload;

    @Column(name = "stroke_count", nullable = false)
    private int strokeCount;

    @Column(name = "received_at", nullable = false)
    private LocalDateTime receivedAt;

    private StrokeBatch(Long writingId, int batchSeq, StrokePayload payload) {
        this.writingId = writingId;
        this.batchSeq = batchSeq;
        this.payload = payload;
        this.strokeCount = payload.strokeCount();
        this.receivedAt = LocalDateTime.now();
    }

    public static StrokeBatch of(Long writingId, int batchSeq, StrokePayload payload) {
        return new StrokeBatch(writingId, batchSeq, payload);
    }
}
