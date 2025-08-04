package com.newnormallist.userservice.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
public class HeaderAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        log.info("Request URI: {}", request.getRequestURI());

        // API Gateway가 전달한 헤더 읽기
        // /api/users 경로는 인증 무시 (permitAll)
        if (path.startsWith("/register")) {
            filterChain.doFilter(request, response);
            return;  // 인증 안 하고 다음 필터로 넘김
        }

        String userId = request.getHeader("X-User-Id");
        String role = request.getHeader("X-User-Role");

        log.info("userId : {}", userId);
        log.info("role : {}", role);

        if (userId != null && role != null) {
            // 이미 Gateway에서 검증된 정보로 인증 객체 구성
            PreAuthenticatedAuthenticationToken authentication =
                    new PreAuthenticatedAuthenticationToken(userId, null,
                            List.of(new SimpleGrantedAuthority(role)));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        filterChain.doFilter(request, response);
    }
}
