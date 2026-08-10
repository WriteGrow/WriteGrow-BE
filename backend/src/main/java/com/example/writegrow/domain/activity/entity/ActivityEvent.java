package com.example.writegrow.domain.activity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "activity_event")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "profile_id", nullable = false)
    private Long profileId;

    @Column(name = "writing_id")
    private Long writingId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private ActivityEventType type;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload")
    private Map<String, Object> payload;

    @Column(name = "occurred_at", nullable = false)
    private LocalDateTime occurredAt;

    private ActivityEvent(ActivityEventType type, Long profileId, Long writingId, Map<String, Object> payload) {
        this.type = type;
        this.profileId = profileId;
        this.writingId = writingId;
        this.payload = payload;
        this.occurredAt = LocalDateTime.now();
    }

    public static ActivityEvent of(ActivityEventType type, Long profileId, Long writingId,
                                   Map<String, Object> payload) {
        return new ActivityEvent(type, profileId, writingId, payload);
    }
}
