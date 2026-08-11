package com.example.writegrow.domain.handwriting.controller;

import com.example.writegrow.domain.handwriting.dto.request.StrokeBatchAppendRequest;
import com.example.writegrow.domain.handwriting.dto.response.HandwritingImageUploadResponse;
import com.example.writegrow.domain.handwriting.dto.response.StrokeBatchAppendResponse;
import com.example.writegrow.domain.handwriting.service.HandwritingService;
import com.example.writegrow.global.common.ApiResponse;
import com.example.writegrow.global.resolver.CurrentProfile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "Handwriting", description = "손글씨 원본 및 작성 과정(획) 데이터 API (REQ-02)")
@RestController
@RequestMapping("/api/writings/{writingId}")
@RequiredArgsConstructor
public class HandwritingController {

    private final HandwritingService handwritingService;

    @Operation(
            summary = "작성 중 획 데이터 전송",
            description = """
                    글을 쓰는 도중 주기적으로 호출해 획과 시각 정보를 보낸다.
                    결과 이미지만으로는 알 수 없는 "어디에서 느려졌는가", "어떤 자모를 어려워하는가"를
                    분석하기 위한 데이터이므로, 제출 전에 반드시 한 번 이상 전송해야 한다.

                    같은 `batchSeq` 로 다시 보내면 중복 저장하지 않고 `duplicated: true` 를 돌려주므로
                    네트워크 오류 시 그대로 재전송하면 된다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수신 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "획 데이터가 비었거나 손글씨 글이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 글이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "글을 찾을 수 없음")
    })
    @PostMapping("/strokes")
    public ResponseEntity<ApiResponse<StrokeBatchAppendResponse>> appendStrokes(
            @CurrentProfile Long profileId,
            @Parameter(description = "글 ID", example = "1") @PathVariable Long writingId,
            @Valid @RequestBody StrokeBatchAppendRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                handwritingService.appendStrokeBatch(profileId, writingId, request)));
    }

    @Operation(
            summary = "손글씨 이미지 업로드",
            description = "아동이 쓴 손글씨를 렌더링한 이미지(PNG/JPEG)를 업로드한다. 제출 전에 한 번 호출해야 한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업로드 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
                    description = "빈 파일이거나 지원하지 않는 형식"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "본인 글이 아님"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "글을 찾을 수 없음")
    })
    @PostMapping(value = "/handwriting-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<HandwritingImageUploadResponse>> uploadImage(
            @CurrentProfile Long profileId,
            @Parameter(description = "글 ID", example = "1") @PathVariable Long writingId,
            @Parameter(description = "손글씨 이미지 파일 (PNG 또는 JPEG)") @RequestPart("file") MultipartFile file,
            @Parameter(description = "캔버스 너비(px)", example = "1024")
            @RequestParam(required = false) Integer canvasWidth,
            @Parameter(description = "캔버스 높이(px)", example = "768")
            @RequestParam(required = false) Integer canvasHeight) {
        return ResponseEntity.ok(ApiResponse.ok(
                handwritingService.uploadImage(profileId, writingId, file, canvasWidth, canvasHeight)));
    }
}
