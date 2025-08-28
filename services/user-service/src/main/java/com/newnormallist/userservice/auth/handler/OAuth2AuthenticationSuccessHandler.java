package com.newnormallist.userservice.auth.handler;

import com.newnormallist.userservice.auth.entity.RefreshToken;
import com.newnormallist.userservice.auth.jwt.JwtTokenProvider;
import com.newnormallist.userservice.auth.repository.RefreshTokenRepository;
import com.newnormallist.userservice.common.ErrorCode;
import com.newnormallist.userservice.common.exception.UserException;
import com.newnormallist.userservice.user.entity.User;
import com.newnormallist.userservice.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @Value("${oauth2.redirect.frontend-callback-url}")
    String frontendCallbackUrl; // 프론트엔드 리다이렉트 URL

//    @Value("${oauth2.redirect.frontend-additional-info-url}")
//    String additionalInfoUrl;

    @Override
    public void onAuthenticationSuccess (HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) throws IOException, ServletException {
        // 1. 인증된 사용자 정보 가져오기
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();

        // 2. 카카오로부터 받은 이메일 정보 추출
        Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
        String email = (String) kakaoAccount.get("email");

        // 3. 이메일 기반으로 우리 DB에서 사용자 조회
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 4. JWT Access Token 및 Refresh Token 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name(), user.getId());
        String refreshTokenValue = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getRole().name(), user.getId(), email);

        // 5. Refresh Token 저장 또는 업데이트
        refreshTokenRepository.findByUserId(user.getId())
                .ifPresentOrElse(
                        refreshToken -> refreshToken.updateTokenValue(refreshTokenValue),
                        () -> refreshTokenRepository.save(new RefreshToken(user, refreshTokenValue))
                );

        // 6. 토큰을 담아 프론트엔드로 리다이렉트
        String targetUrl = createRedirectUrl(accessToken, refreshTokenValue);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String createRedirectUrl(String accessToken, String refreshTokenValue) {
        return UriComponentsBuilder.fromUriString(frontendCallbackUrl)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshTokenValue)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }
}
