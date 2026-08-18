package com.example.writegrow.infra.ai;

import com.example.writegrow.infra.ai.dto.AiAnalysisRequest;
import com.example.writegrow.infra.ai.dto.AiAnalysisResponse;
import com.example.writegrow.infra.ai.dto.AiErrorAnalysisRequest;
import com.example.writegrow.infra.ai.dto.AiErrorAnalysisResponse;

/**
 * 손글씨 OCR 및 작성 과정 분석을 담당하는 AI 서버(WriteGrow-BE/ai) 포트.
 *
 * <p>{@code writegrow.ai.stub=true}(dev 프로파일 기본값)이면 {@link StubAiAnalysisClient} 가 대신 동작하므로
 * AI 서버 없이도 전체 흐름을 검증할 수 있다.
 */
public interface AiAnalysisClient {

    /**
     * 손글씨 이미지와 획 데이터로 OCR 과 작성 과정을 분석한다. (REQ-02)
     */
    AiAnalysisResponse analyze(AiAnalysisRequest request);

    /**
     * 확정된 텍스트에서 오류 후보를 분석한다. (REQ-03)
     */
    AiErrorAnalysisResponse analyzeText(AiErrorAnalysisRequest request);

    /**
     * 분석 결과에 기록할 제공자 이름.
     */
    String provider();
}
