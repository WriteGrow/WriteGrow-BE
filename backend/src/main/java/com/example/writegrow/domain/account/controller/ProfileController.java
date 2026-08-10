package com.example.writegrow.domain.account.controller;

import com.example.writegrow.domain.account.dto.request.ProfileCreateRequest;
import com.example.writegrow.domain.account.dto.response.ProfileResponse;
import com.example.writegrow.domain.account.service.ProfileService;
import com.example.writegrow.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Profile", description = "계정에 속한 사용자 프로필 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @Operation(
            summary = "프로필 생성",
            description = "가족 계정 아래에 아동(CHILD) 또는 보호자(PARENTS) 프로필을 추가한다. "
                    + "글쓰기 API 의 `X-Profile-Id` 헤더에는 여기서 생성된 아동 프로필 ID 를 사용한다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "계정을 찾을 수 없음")
    })
    @PostMapping("/accounts/{accountId}/profiles")
    public ResponseEntity<ApiResponse<ProfileResponse>> createProfile(
            @Parameter(description = "가족 계정 ID", example = "1") @PathVariable Long accountId,
            @Valid @RequestBody ProfileCreateRequest request) {
        ProfileResponse response = profileService.create(accountId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @Operation(
            summary = "계정의 프로필 목록 조회",
            description = """
                    가족 계정에 속한 프로필을 최신 생성순으로 모두 반환한다.
                    한 계정의 프로필 수는 적고 늘어날 여지가 없어 페이지네이션을 두지 않는다.
                    """)
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "계정을 찾을 수 없음")
    })
    @GetMapping("/accounts/{accountId}/profiles")
    public ResponseEntity<ApiResponse<List<ProfileResponse>>> getProfiles(
            @Parameter(description = "가족 계정 ID", example = "1") @PathVariable Long accountId) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getProfilesByAccount(accountId)));
    }

    @Operation(summary = "프로필 단건 조회")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "프로필을 찾을 수 없음")
    })
    @GetMapping("/profiles/{profileId}")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile(
            @Parameter(description = "프로필 ID", example = "1") @PathVariable Long profileId) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getProfile(profileId)));
    }
}
