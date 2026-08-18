package com.example.writegrow.infra.ai;

import com.example.writegrow.global.config.RestClientConfig;
import com.example.writegrow.infra.ai.dto.AiAnalysisRequest;
import com.example.writegrow.infra.ai.dto.AiAnalysisResponse;
import com.example.writegrow.infra.ai.dto.AiErrorAnalysisRequest;
import com.example.writegrow.infra.ai.dto.AiErrorAnalysisResponse;
import com.example.writegrow.infra.ai.exception.AiAnalysisErrorCode;
import com.example.writegrow.infra.ai.exception.AiAnalysisException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * 실제 AI 서버를 호출하는 어댑터. {@code writegrow.ai.stub} 가 false 이거나 지정되지 않았을 때 등록된다.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "writegrow.ai", name = "stub", havingValue = "false", matchIfMissing = true)
public class AiAnalysisHttpClient implements AiAnalysisClient {

    private static final String ANALYZE_PATH = "/handwriting/analyze";
    private static final String TEXT_ANALYZE_PATH = "/text/analyze";
    private static final String PROVIDER = "writegrow-ai";

    private final RestClient restClient;

    public AiAnalysisHttpClient(@Qualifier(RestClientConfig.AI_REST_CLIENT) RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public AiAnalysisResponse analyze(AiAnalysisRequest request) {
        try {
            AiAnalysisResponse response = restClient.post()
                    .uri(ANALYZE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiAnalysisResponse.class);

            if (response == null || response.fullText() == null || response.fullText().isBlank()) {
                throw new AiAnalysisException(AiAnalysisErrorCode.AI_EMPTY_RESPONSE);
            }
            return response;
        } catch (RestClientException exception) {
            log.error("AI 분석 서버 호출 실패: writingId={}", request.writingId(), exception);
            throw new AiAnalysisException(AiAnalysisErrorCode.AI_CALL_FAILED, exception);
        }
    }

    @Override
    public AiErrorAnalysisResponse analyzeText(AiErrorAnalysisRequest request) {
        try {
            AiErrorAnalysisResponse response = restClient.post()
                    .uri(TEXT_ANALYZE_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(AiErrorAnalysisResponse.class);

            if (response == null) {
                throw new AiAnalysisException(AiAnalysisErrorCode.AI_EMPTY_RESPONSE);
            }
            // 오류가 하나도 없는 것은 정상이다. 아동이 맞게 썼다는 뜻이므로 빈 목록을 그대로 돌려준다.
            return response;
        } catch (RestClientException exception) {
            log.error("AI 오류 분석 호출 실패: writingId={}", request.writingId(), exception);
            throw new AiAnalysisException(AiAnalysisErrorCode.AI_CALL_FAILED, exception);
        }
    }

    @Override
    public String provider() {
        return PROVIDER;
    }
}
