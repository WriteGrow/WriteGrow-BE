package com.example.writegrow.domain.analysis.service;

import com.example.writegrow.domain.analysis.dto.response.ErrorProfileResponse;
import com.example.writegrow.domain.analysis.dto.response.ErrorReviewResponse;
import com.example.writegrow.domain.analysis.dto.response.WritingErrorsResponse;

/**
 * 확신도 기반 오류 분석과 반복 오류 프로필. (기능명세서 REQ-03)
 */
public interface ErrorAnalysisService {

    /**
     * 확정된 글의 오류 분석을 수행한다. 최종본이 확정된 뒤 비동기로 호출된다.
     */
    void runAnalysis(Long writingId, Long profileId);

    /**
     * 아동에게 전달할 확정 오류. 낮은 확신도 후보는 포함하지 않는다.
     */
    WritingErrorsResponse getConfirmedErrors(Long profileId, Long writingId);

    /**
     * 보호자·교사가 검토할 낮은 확신도 후보. 확정 오류 개수를 함께 제공해 둘을 구분해 보여줄 수 있게 한다.
     */
    ErrorReviewResponse getReviewCandidates(Long viewerProfileId, Long writingId);

    /**
     * 아동별 누적 반복 오류 프로필.
     */
    ErrorProfileResponse getErrorProfile(Long viewerProfileId, Long childProfileId);
}
