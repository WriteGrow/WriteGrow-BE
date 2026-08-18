package com.example.writegrow.domain.writing.service;

import com.example.writegrow.domain.account.service.ProfileService;
import com.example.writegrow.domain.activity.entity.ActivityEventType;
import com.example.writegrow.domain.activity.service.ActivityEventService;
import com.example.writegrow.domain.handwriting.service.HandwritingQueryService;
import com.example.writegrow.domain.writing.dto.request.WritingCreateRequest;
import com.example.writegrow.domain.writing.dto.request.WritingDraftUpdateRequest;
import com.example.writegrow.domain.writing.dto.request.WritingSubmitRequest;
import com.example.writegrow.domain.writing.dto.request.WritingTextConfirmRequest;
import com.example.writegrow.domain.writing.dto.response.WritingCreateResponse;
import com.example.writegrow.domain.writing.dto.response.WritingDetailResponse;
import com.example.writegrow.domain.writing.dto.response.WritingSubmitResponse;
import com.example.writegrow.domain.writing.dto.response.WritingSummaryResponse;
import com.example.writegrow.domain.writing.dto.response.WritingTextConfirmResponse;
import com.example.writegrow.domain.writing.entity.Writing;
import com.example.writegrow.domain.writing.event.HandwritingSubmittedEvent;
import com.example.writegrow.domain.writing.event.TextConfirmedEvent;
import com.example.writegrow.domain.writing.exception.WritingErrorCode;
import com.example.writegrow.domain.writing.exception.WritingException;
import com.example.writegrow.domain.writing.repository.WritingRepository;
import com.example.writegrow.global.common.PageResponse;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WritingServiceImpl implements WritingService {

    private final WritingRepository writingRepository;
    private final ProfileService profileService;
    private final HandwritingQueryService handwritingQueryService;
    private final ActivityEventService activityEventService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public WritingCreateResponse create(Long profileId, WritingCreateRequest request) {
        // 보호자 동의가 확인된 아동만 글을 시작할 수 있다. (REQ-01 선행조건)
        profileService.getWritableChild(profileId);

        Writing writing = writingRepository.save(
                Writing.create(profileId, request.inputType(), request.topic()));
        return WritingCreateResponse.from(writing);
    }

    @Override
    @Transactional
    public void updateDraft(Long profileId, Long writingId, WritingDraftUpdateRequest request) {
        Writing writing = findOwnedWriting(profileId, writingId);
        writing.updateDraftContent(request.content());
    }

    @Override
    @Transactional
    public WritingSubmitResponse submit(Long profileId, Long writingId, WritingSubmitRequest request) {
        Writing writing = findOwnedWritingWithRevisions(profileId, writingId);

        if (writing.isHandwriting()) {
            writing.submitHandwriting();
            // 커밋 이후 분석 파이프라인이 비동기로 시작된다.
            eventPublisher.publishEvent(new HandwritingSubmittedEvent(writing.getId(), profileId));
        } else {
            writing.submitKeyboard(request == null ? null : request.content());
        }

        activityEventService.record(ActivityEventType.WRITING_SUBMITTED, profileId, writing.getId(),
                Map.of("inputType", writing.getInputType().name()));

        if (!writing.isHandwriting()) {
            activityEventService.record(ActivityEventType.WRITING_CONFIRMED, profileId, writing.getId(),
                    Map.of("edited", false));
            // 키보드 글은 제출 즉시 최종본이 확정되므로 여기서 오류 분석이 시작된다. (REQ-03 트리거)
            eventPublisher.publishEvent(new TextConfirmedEvent(writing.getId(), profileId));
        }
        return WritingSubmitResponse.from(writing);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<WritingSummaryResponse> getWritings(Long profileId, Pageable pageable) {
        return PageResponse.of(
                writingRepository.findAllByProfileId(profileId, pageable),
                WritingSummaryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public WritingDetailResponse getWriting(Long profileId, Long writingId) {
        Writing writing = findOwnedWritingWithRevisions(profileId, writingId);
        return WritingDetailResponse.of(
                writing,
                handwritingQueryService.findByWritingId(writingId).orElse(null));
    }

    @Override
    @Transactional
    public WritingTextConfirmResponse confirmText(Long profileId, Long writingId,
                                                  WritingTextConfirmRequest request) {
        Writing writing = findOwnedWritingWithRevisions(profileId, writingId);
        boolean edited = writing.confirmText(request.content());

        if (edited) {
            activityEventService.record(ActivityEventType.OCR_TEXT_EDITED, profileId, writingId,
                    Map.of("finalTextLength", writing.getFinalText().length()));
        }
        activityEventService.record(ActivityEventType.WRITING_CONFIRMED, profileId, writingId,
                Map.of("edited", edited));

        // 손글씨 글은 아동이 변환 텍스트를 확인한 이 시점에 최종본이 확정된다. (REQ-03 트리거)
        eventPublisher.publishEvent(new TextConfirmedEvent(writingId, profileId));

        return WritingTextConfirmResponse.of(writing, edited);
    }

    private Writing findOwnedWriting(Long profileId, Long writingId) {
        Writing writing = writingRepository.findById(writingId)
                .orElseThrow(() -> new WritingException(WritingErrorCode.WRITING_NOT_FOUND));
        writing.validateOwner(profileId);
        return writing;
    }

    private Writing findOwnedWritingWithRevisions(Long profileId, Long writingId) {
        Writing writing = writingRepository.findWithRevisionsById(writingId)
                .orElseThrow(() -> new WritingException(WritingErrorCode.WRITING_NOT_FOUND));
        writing.validateOwner(profileId);
        return writing;
    }
}
