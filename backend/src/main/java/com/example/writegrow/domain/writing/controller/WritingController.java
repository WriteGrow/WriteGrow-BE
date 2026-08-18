package com.example.writegrow.domain.writing.controller;

import com.example.writegrow.domain.writing.dto.request.WritingCreateRequest;
import com.example.writegrow.domain.writing.dto.request.WritingDraftUpdateRequest;
import com.example.writegrow.domain.writing.dto.request.WritingSubmitRequest;
import com.example.writegrow.domain.writing.dto.request.WritingTextConfirmRequest;
import com.example.writegrow.domain.writing.dto.response.WritingCreateResponse;
import com.example.writegrow.domain.writing.dto.response.WritingDetailResponse;
import com.example.writegrow.domain.writing.dto.response.TodayWritingStatusResponse;
import com.example.writegrow.domain.writing.dto.response.WritingSubmitResponse;
import com.example.writegrow.domain.writing.dto.response.WritingSummaryResponse;
import com.example.writegrow.domain.writing.dto.response.WritingTextConfirmResponse;
import com.example.writegrow.domain.writing.service.WritingService;
import com.example.writegrow.global.common.ApiResponse;
import com.example.writegrow.global.common.PageResponse;
import com.example.writegrow.global.resolver.CurrentProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Writing", description = "아동 자유 글쓰기 API (REQ-01, REQ-02)")
@RestController
@RequestMapping("/api/writings")
@RequiredArgsConstructor
public class WritingController {

    private final WritingService writingService;

    @Operation(
            summary = "글쓰기 시작",
            description = """
                    새 글을 시작한다. 입력 방식(KEYBOARD/PEN)에 따라 이후 흐름이 달라진다.

                    - KEYBOARD: 임시 저장 후 `/submit` 으로 제출하면 즉시 최종본이 확정된다.
                    - PEN: 작성 중 `/strokes` 로 획 데이터를 보내고, 이미지 업로드 후 `/submit` 하면 AI 분석이 시작된다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403",
                    description = "아동 프로필이 아니거나 보호자 동의가 확인되지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로필을 찾을 수 없음")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<WritingCreateResponse>> create(
            @CurrentProfile Long profileId,
            @Valid @RequestBody WritingCreateRequest request) {
        WritingCreateResponse response = writingService.create(profileId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @Operation(summary = "키보드 글 임시 저장", description = "작성 중인 키보드 글의 내용을 저장한다. 손글씨 글에는 사용할 수 없다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "저장 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "입력 방식이 맞지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 글이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "글을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 제출된 글")
    })
    @PatchMapping("/{writingId}")
    public ResponseEntity<Void> updateDraft(
            @CurrentProfile Long profileId,
            @Parameter(description = "글 ID", example = "1") @PathVariable Long writingId,
            @Valid @RequestBody WritingDraftUpdateRequest request) {
        writingService.updateDraft(profileId, writingId, request);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "글 제출",
            description = """
                    글을 제출한다.

                    - 키보드 글: `content` 가 필요하며 빈 글은 제출할 수 없다. 제출 즉시 `CONFIRMED` 가 된다.
                    - 손글씨 글: `content` 없이 호출하며, 응답은 202 와 함께 `SUBMITTED`(분석 진행 중) 상태를 반환한다.
                      이후 `/analysis` 를 폴링해 변환 결과를 확인한다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "키보드 글 제출 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "손글씨 글 제출, 분석 시작"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "빈 글이거나 입력 방식이 맞지 않음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 글이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "글을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 제출된 글")
    })
    @PostMapping("/{writingId}/submit")
    public ResponseEntity<ApiResponse<WritingSubmitResponse>> submit(
            @CurrentProfile Long profileId,
            @Parameter(description = "글 ID", example = "1") @PathVariable Long writingId,
            @RequestBody(required = false) WritingSubmitRequest request) {
        WritingSubmitResponse response = writingService.submit(profileId, writingId, request);
        HttpStatus status = response.analysisInProgress() ? HttpStatus.ACCEPTED : HttpStatus.OK;
        return ResponseEntity.status(status).body(ApiResponse.ok(response));
    }

    @Operation(
            summary = "오늘 작성 현황",
            description = """
                    아동 홈의 "오늘 작성 0 / 1편" 에 쓰는 값이다.

                    작성 중인 글이 있으면 `inProgressWritingId` 가 채워진다. 새 글을 만드는 대신
                    이어쓰게 안내하면 임시 저장만 된 빈 글이 쌓이지 않는다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/today")
    public ResponseEntity<ApiResponse<TodayWritingStatusResponse>> getTodayStatus(
            @CurrentProfile Long profileId) {
        return ResponseEntity.ok(ApiResponse.ok(writingService.getTodayStatus(profileId)));
    }

    @Operation(
            summary = "다시 쓰기",
            description = """
                    아동이 변환 결과를 받아들이지 않고 처음부터 다시 쓴다. ("다시 쓸게요")

                    글을 작성 중(`DRAFT`) 상태로 되돌린다. 새 글을 만들지 않으므로 목록에 미완성 글이
                    남지 않는다. 변환 텍스트와 수정 이력은 지워지지만 **손글씨 원본과 획 데이터는
                    보존된다** — 시도 번호가 올라가 다음 획과 구분될 뿐이다.

                    클라이언트는 `batchSeq` 를 다시 0 부터 보내면 된다.

                    변환이 끝난 손글씨 글에서만 호출할 수 있다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "되돌리기 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "손글씨 글이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 글이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "글을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "변환이 끝나지 않았거나 이미 확정된 글")
    })
    @PostMapping("/{writingId}/rewrite")
    public ResponseEntity<ApiResponse<WritingCreateResponse>> rewrite(
            @CurrentProfile Long profileId,
            @Parameter(description = "글 ID", example = "1") @PathVariable Long writingId) {
        return ResponseEntity.ok(ApiResponse.ok(writingService.rewrite(profileId, writingId)));
    }

    @Operation(
            summary = "이전 글 목록 조회",
            description = """
                    내가 쓴 글을 페이지로 조회한다. 각 항목에 최종 수정본 미리보기가 포함된다.
                    기본 정렬은 최신순(`createdAt` 내림차순)이며, 작성 시각이 같을 때를 대비해 `id` 를 보조 기준으로 사용한다.
                    글은 매일 쌓이므로 이 목록은 페이지네이션이 필수다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "정렬 기준이 올바르지 않음")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<WritingSummaryResponse>>> getWritings(
            @CurrentProfile Long profileId,
            @ParameterObject
            @PageableDefault(size = 20, sort = {"createdAt", "id"}, direction = Sort.Direction.DESC)
            Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(writingService.getWritings(profileId, pageable)));
    }

    @Operation(
            summary = "글 상세 조회",
            description = "원문, 최종 수정본, 수정 이력, 손글씨 원본 정보를 함께 조회한다. 수정 전/후 비교는 `revisions` 를 사용한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 글이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "글을 찾을 수 없음")
    })
    @GetMapping("/{writingId}")
    public ResponseEntity<ApiResponse<WritingDetailResponse>> getWriting(
            @CurrentProfile Long profileId,
            @Parameter(description = "글 ID", example = "1") @PathVariable Long writingId) {
        return ResponseEntity.ok(ApiResponse.ok(writingService.getWriting(profileId, writingId)));
    }

    @Operation(
            summary = "변환 텍스트 확인 및 최종본 확정",
            description = """
                    아동이 OCR 변환 텍스트를 확인하고, 필요하면 고쳐서 최종본으로 확정한다.
                    분석이 끝나 `ANALYZED` 상태가 된 뒤에만 호출할 수 있다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "확정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "빈 글"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 글이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "글을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409",
                    description = "아직 변환이 끝나지 않았거나 이미 확정된 글")
    })
    @PatchMapping("/{writingId}/text")
    public ResponseEntity<ApiResponse<WritingTextConfirmResponse>> confirmText(
            @CurrentProfile Long profileId,
            @Parameter(description = "글 ID", example = "1") @PathVariable Long writingId,
            @Valid @RequestBody WritingTextConfirmRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(writingService.confirmText(profileId, writingId, request)));
    }
}
