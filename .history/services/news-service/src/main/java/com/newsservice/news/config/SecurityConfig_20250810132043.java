package com.newsservice.news.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 비활성화 (REST API이므로)
            .csrf(AbstractHttpConfigurer::disable)

            // 세션 사용하지 않음 (Stateless)
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 모든 요청에 대해 인증 없이 허용
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/**").permitAll()          // API 엔드포인트
                .requestMatchers("/actuator/**").permitAll()     // Actuator 엔드포인트
                .requestMatchers("/health").permitAll()          // 헬스체크
                .requestMatchers("/error").permitAll()           // 에러 페이지
                .anyRequest().permitAll()                        // 그 외 모든 요청
            );

        return http.build();
    }
}
