package com.newsletterservice.config;

import feign.Retryer;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

@Configuration
@Slf4j
public class FeignTimeoutConfig {
    
    /**
     * Feign 재시도 설정
     */
    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(100, 1000, 2); // (period, maxPeriod, maxAttempts)
    }
    
    /**
     * Feign 타임아웃 설정
     */
    @Bean
    public feign.Request.Options feignOptions() {
        return new feign.Request.Options(5000, 10000); // (connectTimeout, readTimeout)
    }
    
    /**
     * Feign 요청에 JWT 토큰 자동 추가
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // 현재 요청에서 Authorization 헤더를 가져와서 Feign 요청에 추가
            String token = getCurrentJwtToken();
            if (token != null && !token.isEmpty()) {
                requestTemplate.header("Authorization", token);
                log.debug("Feign 요청에 Authorization 헤더 추가: {}", token.substring(0, Math.min(token.length(), 20)) + "...");
            } else {
                log.debug("JWT 토큰이 없어 Authorization 헤더를 추가하지 않습니다.");
            }
        };
    }
    
    private String getCurrentJwtToken() {
        try {
            // 현재 HTTP 요청에서 Authorization 헤더 추출
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                String authHeader = request.getHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    return authHeader; // "Bearer " 포함하여 반환
                }
            }
        } catch (Exception e) {
            log.warn("JWT 토큰 추출 중 오류 발생: {}", e.getMessage());
        }
        return null;
    }
}
