package com.example.writegrow.domain.analysis.entity;

import com.example.writegrow.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * 글을 쓰는 "과정"에 대한 분석 지표. REQ-05(개인화 미션), REQ-06(성장 리포트)가 이 데이터를 소비한다.
 */
@Getter
@Entity
@Table(name = "writing_process_metric")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WritingProcessMetric extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "writing_id", nullable = false, unique = true)
    private Long writingId;

    @Column(name = "total_duration_ms")
    private Long totalDurationMs;

    @Column(name = "pause_count")
    private Integer pauseCount;

    @Column(name = "longest_pause_ms")
    private Long longestPauseMs;

    @Column(name = "avg_stroke_duration_ms")
    private Long avgStrokeDurationMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "hesitation_points")
    private List<HesitationPoint> hesitationPoints;

    private WritingProcessMetric(Long writingId) {
        this.writingId = writingId;
    }

    public static WritingProcessMetric create(Long writingId) {
        return new WritingProcessMetric(writingId);
    }

    public void update(Long totalDurationMs, Integer pauseCount, Long longestPauseMs,
                       Long avgStrokeDurationMs, List<HesitationPoint> hesitationPoints) {
        this.totalDurationMs = totalDurationMs;
        this.pauseCount = pauseCount;
        this.longestPauseMs = longestPauseMs;
        this.avgStrokeDurationMs = avgStrokeDurationMs;
        this.hesitationPoints = hesitationPoints;
    }
}
