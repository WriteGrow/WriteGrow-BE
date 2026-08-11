package com.example.writegrow.domain.analysis.service;

import com.example.writegrow.domain.analysis.dto.response.AnalysisResponse;

public interface AnalysisService {

    /**
     * 손글씨 분석 파이프라인을 실행한다. 이벤트 리스너가 비동기로 호출한다.
     *
     * <p>실패해도 예외를 밖으로 던지지 않고 FAILED 상태로 기록한다. 원본은 보존되며 재시도할 수 있다.
     */
    void runAnalysis(Long writingId, Long profileId);

    AnalysisResponse getAnalysis(Long profileId, Long writingId);

    void retryAnalysis(Long profileId, Long writingId);
}
