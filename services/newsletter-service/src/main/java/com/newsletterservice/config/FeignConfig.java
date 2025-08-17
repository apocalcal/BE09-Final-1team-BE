package com.newsletterservice.config;

import com.newsletterservice.common.exception.NewsletterException;
import feign.Logger;
import feign.RequestInterceptor;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;



@Configuration
@EnableFeignClients(basePackages = "com.newsletterservice.client")
@Slf4j
public class FeignConfig {
    
    /**
     * Feign 요청에 JWT 토큰 자동 추가
     */
    @Bean
    public RequestInterceptor requestInterceptor() {
        return requestTemplate -> {
            // JWT 토큰을 헤더에 추가 (Gateway에서 전달받은 토큰 사용)
            String token = getCurrentJwtToken();
            if (token != null) {
                requestTemplate.header("Authorization", "Bearer " + token);
            }
        };
    }
    
    /**
     * Feign 에러 디코더
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new CustomErrorDecoder();
    }
    
    /**
     * Feign 로그 레벨 설정
     */
    @Bean
    Logger.Level feignLoggerLevel() {
        return Logger.Level.HEADERS;
    }
    
    private String getCurrentJwtToken() {
        // SecurityContext에서 현재 토큰 추출
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken) {
            return ((JwtAuthenticationToken) authentication).getToken().getTokenValue();
        }
        return null;
    }
    
    /**
     * 커스텀 에러 디코더
     */
    public static class CustomErrorDecoder implements ErrorDecoder {
        
        private final ErrorDecoder defaultErrorDecoder = new Default();
        
        @Override
        public Exception decode(String methodKey, feign.Response response) {
            switch (response.status()) {
                case 400:
                    return new NewsletterException("Bad Request from " + methodKey, "BAD_REQUEST");
                case 404:
                    return new NewsletterException("Resource not found from " + methodKey, "NOT_FOUND");
                case 503:
                    return new NewsletterException("Service unavailable: " + methodKey, "SERVICE_UNAVAILABLE");
                default:
                    return defaultErrorDecoder.decode(methodKey, response);
            }
        }
    }
}
