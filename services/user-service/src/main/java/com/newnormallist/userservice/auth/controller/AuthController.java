package com.newnormallist.userservice.auth.controller;

import com.newnormallist.userservice.auth.dto.*;
import com.newnormallist.userservice.auth.service.AuthService;
import com.newnormallist.userservice.common.ApiResult;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "인증/인가 및 토큰 관리 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * 로그인 API
     */
    @Operation(
            summary = "로그인",
            description = "이메일/비밀번호로 로그인하여 Access/Refresh 토큰과 사용자 정보를 발급합니다.",
            operationId = "login"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(형식/필수값 누락)"),
            @ApiResponse(responseCode = "401", description = "인증 실패(자격 증명 불일치)"),
            @ApiResponse(responseCode = "429", description = "로그인 시도 횟수 초과(선택)")
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResult<LoginResponseDto>> login(@RequestBody LoginRequestDto loginRequestDto) {
        LoginResponseDto loginResponse = authService.login(loginRequestDto);
        return ResponseEntity.ok(ApiResult.success(loginResponse));
    }

    /**
     * Access Token 갱신 API
     */
    @Operation(
            summary = "토큰 재발급",
            description = "Refresh Token을 검증하여 새로운 Access Token을 발급합니다.",
            operationId = "refreshToken"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(토큰 형식/필수값 누락)"),
            @ApiResponse(responseCode = "401", description = "인증 실패(Refresh Token 무효/만료)")
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResult<AccessTokenResponseDto>> refreshToken(@RequestBody RefreshTokenRequestDto requestDto) {
        AccessTokenResponseDto accessTokenResponseDto = authService.refreshToken(requestDto);
        return ResponseEntity.ok(ApiResult.success(accessTokenResponseDto));
    }

    /**
     * 로그아웃 API
     */
    @Operation(
            summary = "로그아웃",
            description = "발급된 Refresh Token을 무효화하여 로그아웃합니다.",
            operationId = "logout"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그아웃 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(토큰 형식/필수값 누락)")
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResult<String>> logout(@RequestBody RefreshTokenRequestDto requestDto) {
        authService.logout(requestDto);
        return ResponseEntity.ok(ApiResult.success("로그아웃이 성공적으로 완료되었습니다."));
    }

    /**
     * 비밀번호 재설정 메일 발송 요청 API
     */
    @Operation(
            summary = "비밀번호 재설정 메일 요청",
            description = "사용자 식별 정보(예: 이메일)로 비밀번호 재설정 메일을 발송합니다.",
            operationId = "requestPasswordReset"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재설정 메일 발송 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @ApiResponse(responseCode = "404", description = "해당 사용자를 찾을 수 없음")
    })
    @PostMapping("/password/find")
    public ResponseEntity<ApiResult<String>> requestPasswordReset(@Valid @RequestBody PasswordFindRequest request) {
        authService.requestPasswordReset(request);
        return ResponseEntity.ok(ApiResult.success("비밀번호 재설정 메일이 발송되었습니다."));
    }

    /**
     * 비밀번호 재설정 API
     */
    @Operation(
            summary = "비밀번호 재설정",
            description = "메일로 전달된 재설정 토큰과 새 비밀번호로 비밀번호를 변경합니다.",
            operationId = "resetPassword"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "비밀번호 재설정 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청(토큰 만료/형식 오류 등)"),
            @ApiResponse(responseCode = "404", description = "해당 사용자를 찾을 수 없음")
    })
    @PostMapping("/password/reset")
    public ResponseEntity<ApiResult<String>> resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResult.success("비밀번호가 성공적으로 재설정되었습니다."));
    }
}
