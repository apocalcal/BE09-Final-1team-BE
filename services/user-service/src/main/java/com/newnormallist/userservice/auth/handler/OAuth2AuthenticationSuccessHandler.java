package com.newnormallist.userservice.auth.handler;

import com.newnormallist.userservice.auth.entity.RefreshToken;
import com.newnormallist.userservice.auth.jwt.JwtTokenProvider;
import com.newnormallist.userservice.auth.repository.CookieOAuth2AuthorizationRequestRepository;
import com.newnormallist.userservice.auth.repository.RefreshTokenRepository;
import com.newnormallist.userservice.user.entity.User;
import com.newnormallist.userservice.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final CookieOAuth2AuthorizationRequestRepository cookieOAuth2AuthorizationRequestRepository;

    @Value("${oauth2.redirect.frontend-callback-url}")
    private String finalRedirectUrl;

    @Value("${oauth2.redirect.frontend-additional-info-url}")
    private String additionalInfoUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        // OAuth2 로그인 과정에서 사용된 임시 쿠키를 정리합니다.
        clearAuthenticationAttributes(request, response);

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> kakaoAccount = (Map<String, Object>) oAuth2User.getAttributes().get("kakao_account");
        String email = (String) kakaoAccount.get("email");

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("OAuth2 login succeeded but user not found in DB."));

        // 💡 분기 로직: birthYear 또는 gender 정보가 없으면 신규 유저로 판단
        if (user.getBirthYear() == null || user.getGender() == null) {
            log.info("신규 소셜 로그인 사용자입니다. 추가 정보 입력 페이지로 리디렉션합니다. Email: {}", email);

            // 1. 추가 정보 입력을 위한 임시 토큰 발급
            String tempToken = jwtTokenProvider.createTempToken(user.getEmail(), user.getId());

            // 2. 프론트엔드의 추가 정보 입력 페이지로 리다이렉트
            String targetUrl = createAdditionalInfoRedirectUrl(tempToken);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        } else {
            log.info("기존 소셜 로그인 사용자입니다. 최종 로그인 처리를 진행합니다. Email: {}", email);

            // 기존 유저일 경우, 최종 로그인 토큰 발급 및 리다이렉트
            String accessToken = jwtTokenProvider.createAccessToken(user.getEmail(), user.getRole().name(), user.getId());
            String refreshTokenValue = jwtTokenProvider.createRefreshToken(user.getEmail(), user.getRole().name(), user.getId(), email);

            refreshTokenRepository.findByUserId(user.getId())
                    .ifPresentOrElse(
                            refreshToken -> refreshToken.updateTokenValue(refreshTokenValue),
                            () -> refreshTokenRepository.save(new RefreshToken(user, refreshTokenValue))
                    );

            String targetUrl = createFinalRedirectUrl(accessToken, refreshTokenValue);
            getRedirectStrategy().sendRedirect(request, response, targetUrl);
        }
    }
    /**
     * 최종 로그인 성공 시 토큰을 담아 리다이렉트할 URL을 생성합니다.
     */
    private String createFinalRedirectUrl(String accessToken, String refreshToken) {
        return UriComponentsBuilder.fromUriString(finalRedirectUrl)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }
    /**
     * 신규 사용자의 추가 정보 입력을 위해 임시 토큰을 담아 리다이렉트할 URL을 생성합니다.
     */
    private String createAdditionalInfoRedirectUrl(String tempToken) {
        return UriComponentsBuilder.fromUriString(additionalInfoUrl)
                .queryParam("token", tempToken)
                .build()
                .encode(StandardCharsets.UTF_8)
                .toUriString();
    }
    /**
     * 로그인 과정에서 생성된 임시 쿠키를 삭제합니다.
     */
    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
        cookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
    }
}