package com.example.writegrow.domain.analysis.controller;

import com.example.writegrow.domain.analysis.dto.response.AnalysisResponse;
import com.example.writegrow.domain.analysis.service.AnalysisService;
import com.example.writegrow.global.common.ApiResponse;
import com.example.writegrow.global.resolver.CurrentProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Analysis", description = "손글씨 OCR 변환 및 작성 과정 분석 결과 API (REQ-02)")
@RestController
@RequestMapping("/api/writings/{writingId}/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService analysisService;

    @Operation(
            summary = "분석 결과 조회 (폴링)",
            description = """
                    손글씨 제출 후 분석 상태와 결과를 조회한다. 제출 직후에는 `PROCESSING` 이므로
                    `SUCCEEDED` 또는 `FAILED` 가 될 때까지 주기적으로 호출한다.

                    `segments[].lowConfidence` 가 true 인 구절은 확신도가 기준 미만이라
                    아동에게 오류로 확정해 보여주지 않는다. 별도 안내로만 표시해야 한다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "손글씨 글이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 글이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "글 또는 분석 결과를 찾을 수 없음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<AnalysisResponse>> getAnalysis(
            @CurrentProfile Long profileId,
            @Parameter(description = "글 ID", example = "1") @PathVariable Long writingId) {
        return ResponseEntity.ok(ApiResponse.ok(analysisService.getAnalysis(profileId, writingId)));
    }

    @Operation(
            summary = "분석 재시도",
            description = "분석이 실패한 글을 다시 분석한다. 손글씨 원본과 획 데이터는 보존되어 있으므로 재업로드가 필요 없다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "재분석 시작"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 글이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "글 또는 분석 결과를 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "실패 상태가 아님")
    })
    @PostMapping("/retry")
    public ResponseEntity<Void> retryAnalysis(
            @CurrentProfile Long profileId,
            @Parameter(description = "글 ID", example = "1") @PathVariable Long writingId) {
        analysisService.retryAnalysis(profileId, writingId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }
}
