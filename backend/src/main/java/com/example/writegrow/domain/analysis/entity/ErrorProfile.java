package com.example.writegrow.domain.analysis.entity;

import com.example.writegrow.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 아동별·오류 유형별 누적 지표. (기능명세서 REQ-03 기능 1 "오류 프로필")
 *
 * <p>여기에는 <b>확정 오류만</b> 쌓인다. 낮은 확신도 후보를 집계하면 아동이 실제로 틀리지 않은
 * 것이 반복 오류로 잡히고, 그 값이 미션과 리포트로 흘러간다. 명세가 이를 금지한다.
 *
 * <p>기간별 추이는 컬럼으로 두지 않는다. {@code ErrorCandidate} 를 날짜로 집계하면 같은 값을
 * 얻을 수 있고, 중복해서 들고 있으면 두 값이 어긋날 때 어느 쪽이 맞는지 알 수 없다.
 */
@Getter
@Entity
@Table(name = "error_profile",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_error_profile_profile_type",
                columnNames = {"profile_id", "error_type"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ErrorProfile extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Enumerated(EnumType.STRING)
    @Column(name = "error_type", nullable = false, length = 30)
    private ErrorType errorType;

    @Column(name = "occurrence_count", nullable = false)
    private int occurrenceCount;

    /** 아동이 스스로 고쳐낸 횟수. REQ-04(단계적 힌트)가 붙으면 여기서 올라간다. */
    @Column(name = "correction_success_count", nullable = false)
    private int correctionSuccessCount;

    @Column(name = "last_occurred_on")
    private LocalDate lastOccurredOn;

    private ErrorProfile(Long profileId, ErrorType errorType) {
        this.profileId = profileId;
        this.errorType = errorType;
        this.occurrenceCount = 0;
        this.correctionSuccessCount = 0;
    }

    public static ErrorProfile create(Long profileId, ErrorType errorType) {
        return new ErrorProfile(profileId, errorType);
    }

    public void recordOccurrences(int count, LocalDate occurredOn) {
        if (count <= 0) {
            return;
        }
        this.occurrenceCount += count;
        if (lastOccurredOn == null || occurredOn.isAfter(lastOccurredOn)) {
            this.lastOccurredOn = occurredOn;
        }
    }

    public void recordCorrectionSuccess() {
        this.correctionSuccessCount++;
    }

    /**
     * 자기교정 성공률(0.0 ~ 1.0). 발생이 없으면 0 이다.
     */
    public double correctionRate() {
        return occurrenceCount == 0 ? 0.0 : (double) correctionSuccessCount / occurrenceCount;
    }
}
