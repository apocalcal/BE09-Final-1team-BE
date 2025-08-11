package com.newnormallist.adminservice.config;

import com.newnormallist.adminservice.security.CurrentRequestTokenResolver;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignAuthConfig implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        // SecurityContext에서 토큰 추출
        String token = CurrentRequestTokenResolver.resolve();
        if (token != null & !token.isBlank()) {
            template.header("Authorization", "Bearer " + token);
        }
    }
}
