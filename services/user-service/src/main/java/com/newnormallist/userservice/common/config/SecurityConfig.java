package com.newnormallist.userservice.common.config;

import com.newnormallist.userservice.auth.jwt.HeaderAuthenticationFilter;
import com.newnormallist.userservice.auth.jwt.RestAccessDeniedHandler;
import com.newnormallist.userservice.auth.jwt.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final HeaderAuthenticationFilter headerAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    // PasswordEncoder Bean 등록
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 1. 기본적인 stateless 설정 (CSRF, 폼 로그인 비활성화)
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 2. 예외 처리 핸들러 설정
                .exceptionHandling(e ->
                        e.authenticationEntryPoint(restAuthenticationEntryPoint)
                                .accessDeniedHandler(restAccessDeniedHandler))
                // 3. 인가 규칙 설정
                .authorizeHttpRequests(auth ->
                        // 게이트웨이를 통과한 모든 요청을 신뢰하므로, 모든 요청을 허용
                        auth.anyRequest().permitAll()
                )
                // 4. 게이트웨이가 추가한 헤더를 처리하는 커스텀 필터는 유지합니다.
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        // 5. .cors() 설정 및 CorsConfigurationSource Bean은 완전히 제거되었습니다.
        return http.build();
    }
}