package com.example.writegrow.domain.analysis.listener;

import com.example.writegrow.domain.analysis.service.AnalysisService;
import com.example.writegrow.domain.writing.event.HandwritingSubmittedEvent;
import com.example.writegrow.global.config.AsyncConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 손글씨 제출 트랜잭션이 커밋된 뒤 분석 파이프라인을 비동기로 시작한다.
 *
 * <p>커밋 이후에 시작하므로 분석 스레드가 아직 저장되지 않은 글을 읽는 일이 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HandwritingSubmittedEventListener {

    private final AnalysisService analysisService;

    @Async(AsyncConfig.ANALYSIS_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleHandwritingSubmitted(HandwritingSubmittedEvent event) {
        log.info("손글씨 분석 시작: writingId={}", event.writingId());
        analysisService.runAnalysis(event.writingId(), event.profileId());
    }
}
