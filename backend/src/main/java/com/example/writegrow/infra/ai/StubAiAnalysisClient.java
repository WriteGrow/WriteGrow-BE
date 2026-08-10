package com.example.writegrow.infra.ai;

import com.example.writegrow.infra.ai.dto.AiAnalysisRequest;
import com.example.writegrow.infra.ai.dto.AiAnalysisResponse;
import com.example.writegrow.infra.ai.dto.AiAnalysisResponse.HesitationPoint;
import com.example.writegrow.infra.ai.dto.AiAnalysisResponse.ProcessMetric;
import com.example.writegrow.infra.ai.dto.AiAnalysisResponse.Segment;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * AI 서버가 준비되기 전까지 전체 흐름을 검증하기 위한 대역. {@code writegrow.ai.stub=true} 일 때만 등록된다.
 *
 * <p>낮은 확신도 구절이 하나 포함된 고정 결과를 돌려주므로, 확신도 임계값 처리와 아동 수정 흐름을 그대로 확인할 수 있다.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "writegrow.ai", name = "stub", havingValue = "true")
public class StubAiAnalysisClient implements AiAnalysisClient {

    private static final String PROVIDER = "stub";
    private static final String FULL_TEXT = "오늘 학교에서 친구랑 놀앗다";

    @Override
    public AiAnalysisResponse analyze(AiAnalysisRequest request) {
        log.info("[STUB] AI 분석 요청을 대신 처리합니다: writingId={}, strokeUrl={}",
                request.writingId(), request.strokeUrl());

        return new AiAnalysisResponse(
                FULL_TEXT,
                0.88,
                List.of(
                        new Segment("오늘", 0.97, 0, 2),
                        new Segment("학교에서", 0.94, 3, 7),
                        // 임계값(기본 0.7) 미만이라 아동 교정 대상에서 제외되고 검토 대상으로만 분리된다.
                        new Segment("놀앗다", 0.62, 12, 15)),
                new ProcessMetric(
                        92000L,
                        4,
                        7300L,
                        410L,
                        List.of(new HesitationPoint(12, "놀", "ㄴ", 5200L, 2))));
    }

    @Override
    public String provider() {
        return PROVIDER;
    }
}
