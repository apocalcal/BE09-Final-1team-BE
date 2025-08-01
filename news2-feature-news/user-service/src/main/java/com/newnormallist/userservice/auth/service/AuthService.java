package com.newnormallist.userservice.auth.service;

import com.newnormallist.userservice.auth.dto.AccessTokenResponseDto;
import com.newnormallist.userservice.auth.dto.LoginRequestDto;
import com.newnormallist.userservice.auth.dto.RefreshTokenRequestDto;
import com.newnormallist.userservice.auth.dto.TokenResponseDto;
import com.newnormallist.userservice.auth.entity.RefreshToken;
import com.newnormallist.userservice.auth.jwt.JwtTokenProvider;
import com.newnormallist.userservice.auth.repository.RefreshTokenRepository;
import com.newnormallist.userservice.common.ErrorCode;
import com.newnormallist.userservice.user.entity.User;
import com.newnormallist.userservice.user.exception.UserException;
import com.newnormallist.userservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public TokenResponseDto login(LoginRequestDto loginRequestDto) {
        // 1. 사용자 조회 및 비밀번호 검증
        User user = userRepository.findByEmail(loginRequestDto.getEmail())
                .filter(u -> passwordEncoder.matches(loginRequestDto.getPassword(), u.getPassword()))
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND)); // 로그인 실패 시 USER_NOT_FOUND 사용
        // 2. Access Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name(), user.getId());
        // 3. Refresh Token 생성
        String refreshTokenValue = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getRole().name(), user.getId(), loginRequestDto.getDeviceId());
        // 4. Refresh Token 저장 또는 업데이트
        refreshTokenRepository.findByUserId(user.getId())
                .ifPresentOrElse(
                        // 이미 Refresh Token이 있으면 값 업데이트
                        refreshToken -> refreshToken.updateTokenValue(refreshTokenValue),
                        // 없으면 새로 생성하여 저장
                        () -> refreshTokenRepository.save(new RefreshToken(user, refreshTokenValue))
                );
        // 5. 토큰을 DTO에 담아 반환
        return new TokenResponseDto(accessToken, refreshTokenValue);
    }

    @Transactional
    public AccessTokenResponseDto refreshToken(RefreshTokenRequestDto request) {
        // 1. Refresh Token 유효성 검증
        if (!jwtTokenProvider.validateToken(request.getRefreshToken())) {
            throw new UserException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        // 2. Refresh Token 조회
        RefreshToken refreshToken = refreshTokenRepository.findByTokenValue(request.getRefreshToken())
                .orElseThrow(() -> new UserException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));
        // 3. Refresh Token의 사용자 정보 조회 및 새로운 Access Token 생성
        User user = refreshToken.getUser();
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name(), user.getId());
        // 4. 새로운 Access Token을 DTO에 담아 반환
        return new AccessTokenResponseDto(newAccessToken);
    }
}
