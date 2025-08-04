package com.newnormallist.userservice.auth.controller;

import com.newnormallist.userservice.auth.dto.AccessTokenResponseDto;
import com.newnormallist.userservice.auth.dto.LoginRequestDto;
import com.newnormallist.userservice.auth.dto.RefreshTokenRequestDto;
import com.newnormallist.userservice.auth.dto.TokenResponseDto;
import com.newnormallist.userservice.auth.service.AuthService;
import com.newnormallist.userservice.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    /**
     * 로그인 API
     * @param loginRequestDto 로그인 요청 DTO
     * @return Access Token과 Refresh Token을 포함한 응답
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<TokenResponseDto>> login(@RequestBody LoginRequestDto loginRequestDto) {
        TokenResponseDto tokenResponse = authService.login(loginRequestDto);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse));
    }
    /**
     * Access Token 갱신 API
     * @param requestDto Refresh Token 요청 DTO
     * @return 갱신된 Access Token 반환
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AccessTokenResponseDto>> refreshToken(@RequestBody RefreshTokenRequestDto requestDto) {
        AccessTokenResponseDto accessTokenResponseDto = authService.refreshToken(requestDto);
        return ResponseEntity.ok(ApiResponse.success(accessTokenResponseDto));
    }
    /**
     * 로그아웃 API
     * @param requestDto Refresh Token 요청 DTO
     * @return 로그아웃 성공 메시지
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(@RequestBody RefreshTokenRequestDto requestDto) {
        authService.logout(requestDto);
        return ResponseEntity.ok(ApiResponse.success("로그아웃이 성공적으로 완료되었습니다."));
    }
}
