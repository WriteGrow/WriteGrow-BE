package com.example.writegrow.domain.activity.service;

import com.example.writegrow.domain.activity.entity.ActivityEventType;
import java.util.Map;

/**
 * 학습 활동 이벤트를 기록하는 단일 지점.
 */
public interface ActivityEventService {

    void record(ActivityEventType type, Long profileId, Long writingId, Map<String, Object> payload);

    default void record(ActivityEventType type, Long profileId, Long writingId) {
        record(type, profileId, writingId, Map.of());
    }
}
