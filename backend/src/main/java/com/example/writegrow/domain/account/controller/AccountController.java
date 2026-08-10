package com.example.writegrow.domain.account.controller;

import com.example.writegrow.domain.account.dto.request.AccountCreateRequest;
import com.example.writegrow.domain.account.dto.response.AccountResponse;
import com.example.writegrow.domain.account.service.AccountService;
import com.example.writegrow.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Account", description = "가족 계정 API")
@RestController
@RequestMapping("/api/accounts")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @Operation(
            summary = "가족 계정 생성",
            description = "하나의 가족 계정을 만든다. 계정 아래에 아동/보호자 프로필을 추가할 수 있다. "
                    + "로그인 기능은 MVP 범위 밖이라 인증 없이 생성된다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "요청 값 검증 실패")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<AccountResponse>> createAccount(
            @Valid @RequestBody AccountCreateRequest request) {
        AccountResponse response = accountService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }
}
