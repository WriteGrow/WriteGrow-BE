package com.example.writegrow.domain.writing.service;

import com.example.writegrow.domain.writing.dto.request.WritingCreateRequest;
import com.example.writegrow.domain.writing.dto.request.WritingDraftUpdateRequest;
import com.example.writegrow.domain.writing.dto.request.WritingSubmitRequest;
import com.example.writegrow.domain.writing.dto.request.WritingTextConfirmRequest;
import com.example.writegrow.domain.writing.dto.response.WritingCreateResponse;
import com.example.writegrow.domain.writing.dto.response.WritingDetailResponse;
import com.example.writegrow.domain.writing.dto.response.TodayWritingStatusResponse;
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

    /**
     * 아동 홈의 오늘 작성 현황.
     */
    TodayWritingStatusResponse getTodayStatus(Long profileId);

    /**
     * 변환 결과를 받아들이지 않고 처음부터 다시 쓴다. ("다시 쓸게요")
     *
     * <p>글을 작성 중 상태로 되돌린다. 새 글을 만들지 않으므로 목록에 미완성 글이 남지 않는다.
     */
    WritingCreateResponse rewrite(Long profileId, Long writingId);
}
