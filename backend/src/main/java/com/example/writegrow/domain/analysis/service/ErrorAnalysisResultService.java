package com.example.writegrow.domain.analysis.service;

import com.example.writegrow.infra.ai.dto.AiErrorAnalysisResponse;

/**
 * 오류 분석 결과를 저장하는 짧은 트랜잭션들. AI 호출을 트랜잭션 밖에 두기 위해 분리했다.
 */
public interface ErrorAnalysisResultService {

    void markProcessing(Long writingId, Long profileId, String analyzedText);

    void completeAnalysis(Long writingId, Long profileId, String provider, AiErrorAnalysisResponse response);

    void failAnalysis(Long writingId, Long profileId, String reason);
}
