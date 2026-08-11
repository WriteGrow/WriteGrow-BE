package com.example.writegrow.domain.writing.service;

import com.example.writegrow.domain.writing.dto.request.WritingCreateRequest;
import com.example.writegrow.domain.writing.dto.request.WritingDraftUpdateRequest;
import com.example.writegrow.domain.writing.dto.request.WritingSubmitRequest;
import com.example.writegrow.domain.writing.dto.request.WritingTextConfirmRequest;
import com.example.writegrow.domain.writing.dto.response.WritingCreateResponse;
import com.example.writegrow.domain.writing.dto.response.WritingDetailResponse;
import com.example.writegrow.domain.writing.dto.response.WritingSubmitResponse;
import com.example.writegrow.domain.writing.dto.response.WritingSummaryResponse;
import com.example.writegrow.domain.writing.dto.response.WritingTextConfirmResponse;
import com.example.writegrow.global.common.PageResponse;
import org.springframework.data.domain.Pageable;

public interface WritingService {

    WritingCreateResponse create(Long profileId, WritingCreateRequest request);

    void updateDraft(Long profileId, Long writingId, WritingDraftUpdateRequest request);

    WritingSubmitResponse submit(Long profileId, Long writingId, WritingSubmitRequest request);

    PageResponse<WritingSummaryResponse> getWritings(Long profileId, Pageable pageable);

    WritingDetailResponse getWriting(Long profileId, Long writingId);

    WritingTextConfirmResponse confirmText(Long profileId, Long writingId, WritingTextConfirmRequest request);
}
