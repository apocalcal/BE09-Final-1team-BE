package com.newnormallist.newsservice.news.config;

import com.newnormallist.newsservice.news.config.auth.HeaderAuthenticationFilter;
import com.newnormallist.newsservice.news.config.auth.RestAccessDeniedHandler;
import com.newnormallist.newsservice.news.config.auth.RestAuthenticationEntryPoint;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final HeaderAuthenticationFilter headerAuthenticationFilter;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final RestAccessDeniedHandler restAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 1. 기본 설정 비활성화
                .csrf(AbstractHttpConfigurer::disable) // CSRF 보호 비활성화 (stateless API 서버이므로)
                .formLogin(AbstractHttpConfigurer::disable) // 폼 로그인 비활성화
                .httpBasic(AbstractHttpConfigurer::disable) // HTTP Basic 인증 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션을 사용하지 않음

                // 2. 예외 처리 핸들러 설정
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(restAuthenticationEntryPoint) // 인증 실패 시 처리
                        .accessDeniedHandler(restAccessDeniedHandler) // 인가(권한) 실패 시 처리
                )

                // 3. 경로별 접근 권한 설정 (가장 중요: Default-Deny 정책)
                .authorizeHttpRequests(authz -> authz
                        // 3-1. 누구나 접근 가능한 경로 (인증 불필요)
                        .requestMatchers(
                                "/actuator/**",
                                "/health",
                                "/error",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/news-api-docs/**"
                        ).permitAll()

                        // 3-2. 컬렉션, 마이페이지 관련 경로는 인증 필요
                        .requestMatchers("/api/collections/**", "/api/news/mypage/**").authenticated()

                        // 3-3. 뉴스 목록, 상세 조회 등 GET 요청은 누구나 가능하도록 허용
                        .requestMatchers(HttpMethod.GET, "/api/news/**").permitAll()

                        // 3-4. 위에서 명시적으로 허용한 경로 외의 모든 요청은 반드시 인증이 필요함
                        .anyRequest().authenticated()
                )

                // 4. 커스텀 필터 추가
                // 직접 만든 HeaderAuthenticationFilter를 Spring Security의 기본 필터 체인에 추가
                .addFilterBefore(headerAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
