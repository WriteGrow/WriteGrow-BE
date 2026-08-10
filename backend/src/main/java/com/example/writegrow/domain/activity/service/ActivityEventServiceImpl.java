package com.example.writegrow.domain.activity.service;

import com.example.writegrow.domain.activity.entity.ActivityEvent;
import com.example.writegrow.domain.activity.entity.ActivityEventType;
import com.example.writegrow.domain.activity.repository.ActivityEventRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ActivityEventServiceImpl implements ActivityEventService {

    private final ActivityEventRepository activityEventRepository;

    @Override
    @Transactional
    public void record(ActivityEventType type, Long profileId, Long writingId, Map<String, Object> payload) {
        activityEventRepository.save(ActivityEvent.of(type, profileId, writingId, payload));
    }
}
