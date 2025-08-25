package com.newsletterservice.config;

import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
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
}
