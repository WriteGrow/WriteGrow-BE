package com.example.writegrow.domain.report.controller;

import com.example.writegrow.domain.report.dto.response.ChildWritingDetailResponse;
import com.example.writegrow.domain.report.dto.response.ParentHomeResponse;
import com.example.writegrow.domain.report.dto.response.WeeklyReportResponse;
import com.example.writegrow.domain.report.service.WeeklyReportService;
import com.example.writegrow.domain.writing.dto.response.WritingSummaryResponse;
import com.example.writegrow.global.common.ApiResponse;
import com.example.writegrow.global.common.PageResponse;
import com.example.writegrow.global.resolver.CurrentProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Parent Report", description = "보호자 주간 성장 리포트와 글 기록 열람 API (REQ-06)")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ParentReportController {

    private final WeeklyReportService weeklyReportService;

    @Operation(
            summary = "보호자 홈",
            description = """
                    같은 계정에 속한 아동들의 이번 주 현황을 카드로 조회한다.
                    카드 순서는 등록 순으로 고정되므로 조회할 때마다 자리가 바뀌지 않는다.

                    오류 수는 확정 오류만 센 값이다. 낮은 확신도 후보는 포함되지 않는다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로필을 찾을 수 없음")
    })
    @GetMapping("/parents/home")
    public ResponseEntity<ApiResponse<ParentHomeResponse>> getParentHome(
            @CurrentProfile Long viewerProfileId) {
        return ResponseEntity.ok(ApiResponse.ok(weeklyReportService.getParentHome(viewerProfileId)));
    }

    @Operation(
            summary = "주간 성장 리포트",
            description = """
                    자녀의 주간 작성 현황, 반복 오류, 변화 추이, 자기교정 현황, 다음 집중 영역을 조회한다.

                    주는 월요일에 시작한다(ISO 8601). `weekOf` 에 해당 주의 아무 날짜나 주면 그 주를
                    조회하고, 생략하면 이번 주다.

                    - **낮은 확신도 후보는 집계에서 제외된다.** 검토 대기 건수만 별도로 제공한다.
                    - 해당 주에 글이 없으면 `hasWriting: false` 로 작성 공백을 알린다.
                    - 일자별 추이는 글이 없는 날도 0 으로 채워 일곱 칸을 모두 돌려준다.
                    - `nextFocus` 는 유형과 근거 코드만 제공한다. 전문적인 진단 문구는 만들지 않는다.

                    조회 시 열람 이벤트가 기록된다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "연결되지 않은 아동"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로필을 찾을 수 없음")
    })
    @GetMapping("/children/{childProfileId}/weekly-report")
    public ResponseEntity<ApiResponse<WeeklyReportResponse>> getWeeklyReport(
            @CurrentProfile Long viewerProfileId,
            @Parameter(description = "아동 프로필 ID", example = "1") @PathVariable Long childProfileId,
            @Parameter(description = "조회할 주에 속한 날짜. 생략하면 이번 주", example = "2026-08-18")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate weekOf) {
        return ResponseEntity.ok(ApiResponse.ok(
                weeklyReportService.getWeeklyReport(viewerProfileId, childProfileId, weekOf)));
    }

    @Operation(
            summary = "자녀 글 목록",
            description = "자녀가 쓴 글을 페이지로 조회한다. 기본 정렬은 최신순이며 보조 기준으로 `id` 를 쓴다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "연결되지 않은 아동"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로필을 찾을 수 없음")
    })
    @GetMapping("/children/{childProfileId}/writings")
    public ResponseEntity<ApiResponse<PageResponse<WritingSummaryResponse>>> getChildWritings(
            @CurrentProfile Long viewerProfileId,
            @Parameter(description = "아동 프로필 ID", example = "1") @PathVariable Long childProfileId,
            @ParameterObject
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(
                weeklyReportService.getChildWritings(viewerProfileId, childProfileId, pageable)));
    }

    @Operation(
            summary = "글 수정 전후 열람",
            description = """
                    자녀가 쓴 개별 글의 수정 전 원문과 최종 수정본을 함께 조회한다.
                    교정 유형은 확정 오류에서 읽고, 낮은 확신도 후보는 개수만 알려준다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "연결되지 않은 아동이거나 해당 아동의 글이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "글을 찾을 수 없음")
    })
    @GetMapping("/children/{childProfileId}/writings/{writingId}")
    public ResponseEntity<ApiResponse<ChildWritingDetailResponse>> getChildWriting(
            @CurrentProfile Long viewerProfileId,
            @Parameter(description = "아동 프로필 ID", example = "1") @PathVariable Long childProfileId,
            @Parameter(description = "글 ID", example = "1") @PathVariable Long writingId) {
        return ResponseEntity.ok(ApiResponse.ok(
                weeklyReportService.getChildWriting(viewerProfileId, childProfileId, writingId)));
    }
}
