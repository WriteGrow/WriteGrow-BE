package com.example.writegrow.domain.analysis.listener;

import com.example.writegrow.domain.analysis.service.ErrorAnalysisService;
import com.example.writegrow.domain.writing.event.TextConfirmedEvent;
import com.example.writegrow.global.config.AsyncConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 최종본이 확정된 뒤 오류 분석을 비동기로 시작한다. (REQ-03 트리거)
 *
 * <p>커밋 이후에 시작하므로 분석 스레드가 아직 확정되지 않은 텍스트를 읽는 일이 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TextConfirmedEventListener {

    private final ErrorAnalysisService errorAnalysisService;

    @Async(AsyncConfig.ANALYSIS_EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTextConfirmed(TextConfirmedEvent event) {
        log.info("오류 분석 시작: writingId={}", event.writingId());
        errorAnalysisService.runAnalysis(event.writingId(), event.profileId());
    }
}
