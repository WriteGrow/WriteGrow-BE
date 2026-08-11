package com.example.writegrow.domain.handwriting.entity;

import com.example.writegrow.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 손글씨 원본 자산. 이미지와 병합된 획 데이터의 S3 키, 그리고 과정 요약 지표를 보관한다.
 *
 * <p>OCR 이 실패해도 이 레코드와 S3 객체는 삭제하지 않는다. (기능명세서 FEAT-02-01 예외 규칙)
 */
@Getter
@Entity
@Table(name = "handwriting_asset")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HandwritingAsset extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "writing_id", nullable = false, unique = true)
    private Long writingId;

    @Column(name = "image_object_key", length = 500)
    private String imageObjectKey;

    @Column(name = "stroke_object_key", length = 500)
    private String strokeObjectKey;

    @Column(name = "canvas_width")
    private Integer canvasWidth;

    @Column(name = "canvas_height")
    private Integer canvasHeight;

    @Column(name = "total_duration_ms")
    private Long totalDurationMs;

    @Column(name = "stroke_count")
    private Integer strokeCount;

    private HandwritingAsset(Long writingId) {
        this.writingId = writingId;
    }

    public static HandwritingAsset create(Long writingId) {
        return new HandwritingAsset(writingId);
    }

    public void updateImage(String imageObjectKey, Integer canvasWidth, Integer canvasHeight) {
        this.imageObjectKey = imageObjectKey;
        if (canvasWidth != null) {
            this.canvasWidth = canvasWidth;
        }
        if (canvasHeight != null) {
            this.canvasHeight = canvasHeight;
        }
    }

    public void updateStrokeData(String strokeObjectKey, int strokeCount, long totalDurationMs) {
        this.strokeObjectKey = strokeObjectKey;
        this.strokeCount = strokeCount;
        this.totalDurationMs = totalDurationMs;
    }

    public boolean hasImage() {
        return imageObjectKey != null;
    }

    public boolean hasStrokeData() {
        return strokeObjectKey != null;
    }
}
