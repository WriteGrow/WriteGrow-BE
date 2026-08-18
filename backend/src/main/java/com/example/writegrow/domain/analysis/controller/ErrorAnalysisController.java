package com.example.writegrow.domain.analysis.controller;

import com.example.writegrow.domain.analysis.dto.response.ErrorProfileResponse;
import com.example.writegrow.domain.analysis.dto.response.ErrorReviewResponse;
import com.example.writegrow.domain.analysis.dto.response.WritingErrorsResponse;
import com.example.writegrow.domain.analysis.service.ErrorAnalysisService;
import com.example.writegrow.global.common.ApiResponse;
import com.example.writegrow.global.resolver.CurrentProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Error Analysis", description = "확신도 기반 오류 분석과 반복 오류 프로필 API (REQ-03)")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ErrorAnalysisController {

    private final ErrorAnalysisService errorAnalysisService;

    @Operation(
            summary = "확정 오류 조회 (아동)",
            description = """
                    아동에게 보여줄 교정 대상 오류를 조회한다.

                    **낮은 확신도 후보는 포함되지 않으며 개수조차 내려주지 않는다.** 자연스러운 표현을
                    오류로 강제 교정하지 않기 위한 규칙이라, 아동이 "숨겨진 오류가 있다"고 인지하는 것도 막는다.

                    최종본이 확정된 뒤 비동기로 분석이 시작되므로, `status` 가 `PROCESSING` 이면 잠시 후 다시 조회한다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 글이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "글이 없거나 아직 분석 결과가 없음")
    })
    @GetMapping("/writings/{writingId}/errors")
    public ResponseEntity<ApiResponse<WritingErrorsResponse>> getConfirmedErrors(
            @CurrentProfile Long profileId,
            @Parameter(description = "글 ID", example = "1") @PathVariable Long writingId) {
        return ResponseEntity.ok(ApiResponse.ok(
                errorAnalysisService.getConfirmedErrors(profileId, writingId)));
    }

    @Operation(
            summary = "낮은 확신도 오류 검토 대상 조회 (보호자·교사)",
            description = """
                    AI 판단 확신도가 운영 기준 미만이라 아동 교정 대상에서 제외된 후보를 조회한다.
                    확정 오류 개수를 함께 내려주므로 둘을 구분해 표시할 수 있다.

                    이 후보들은 아동의 확정 오류 통계와 반복 오류 프로필에 반영되지 않았다.
                    조회 시 열람 이벤트가 기록된다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "연결되지 않은 아동의 글"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404",
                    description = "글이 없거나 아직 분석 결과가 없음")
    })
    @GetMapping("/writings/{writingId}/error-review")
    public ResponseEntity<ApiResponse<ErrorReviewResponse>> getReviewCandidates(
            @CurrentProfile Long viewerProfileId,
            @Parameter(description = "글 ID", example = "1") @PathVariable Long writingId) {
        return ResponseEntity.ok(ApiResponse.ok(
                errorAnalysisService.getReviewCandidates(viewerProfileId, writingId)));
    }

    @Operation(
            summary = "반복 오류 프로필 조회 (보호자·교사)",
            description = """
                    아동의 오류 유형별 누적 지표를 발생이 잦은 순으로 조회한다.
                    주간 리포트의 "주요 반복 오류"와 "다음 집중 영역"의 근거가 되는 값이다.

                    **확정 오류만 집계된 값이다.** 낮은 확신도 후보는 포함되지 않는다.
                    유형이 여섯 가지로 고정이라 페이지네이션을 두지 않는다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "연결되지 않은 아동"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로필을 찾을 수 없음")
    })
    @GetMapping("/children/{childProfileId}/error-profile")
    public ResponseEntity<ApiResponse<ErrorProfileResponse>> getErrorProfile(
            @CurrentProfile Long viewerProfileId,
            @Parameter(description = "아동 프로필 ID", example = "1") @PathVariable Long childProfileId) {
        return ResponseEntity.ok(ApiResponse.ok(
                errorAnalysisService.getErrorProfile(viewerProfileId, childProfileId)));
    }
}
