package com.example.writegrow.domain.analysis.service;

import com.example.writegrow.infra.ai.dto.AiAnalysisResponse;

/**
 * 분석 파이프라인의 각 단계를 독립된 트랜잭션으로 기록한다.
 *
 * <p>AI 호출은 오래 걸리므로 호출 구간을 트랜잭션 밖에 두고, 상태 변경만 짧은 트랜잭션으로 나눈다.
 * 실패 기록이 앞 단계의 롤백에 휩쓸리지 않게 하려는 목적도 있다.
 */
public interface AnalysisResultService {

    void markProcessing(Long writingId, Long profileId);

    void completeAnalysis(Long writingId, Long profileId, String provider, AiAnalysisResponse response);

    void failAnalysis(Long writingId, Long profileId, String reason);
}
