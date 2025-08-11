package com.newnormallist.adminservice.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class CurrentRequestTokenResolver {
    public static String resolve() {
        // 1. 현재 요청의 SecurityContext에서 인증 정보를 가져옴
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof String s && !s.isBlank()) {
            return s;
        }
        return null;
    }
}
