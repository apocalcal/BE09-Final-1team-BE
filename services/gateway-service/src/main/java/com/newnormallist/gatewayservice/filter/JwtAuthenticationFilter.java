package com.newnormallist.gatewayservice.filter;

import com.newnormallist.gatewayservice.jwt.GatewayJwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

  private final GatewayJwtTokenProvider jwtTokenProvider;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getURI().getPath();
    log.info("✅ [Gateway] Request Path: {}", path);

    // permitAll 경로는 토큰 검증 없이 통과
    if (isPermitAllPath(path)) {
      log.info("✅ [Gateway] PermitAll path, skipping token validation for path: {}", path);
      return chain.filter(exchange);
    }

    // Authorization 헤더에서 토큰 추출
    String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      log.error("❌ [Gateway] Authorization header is missing or does not start with Bearer.");
      return handleUnauthorized(exchange, "Authorization header is missing or invalid.");
    }

    String token = authHeader.substring(7);
    log.info("✅ [Gateway] Token found. Processing token...");

    try {
      // 토큰 유효성 검증
      if (!jwtTokenProvider.validateToken(token)) {
        // validateToken 내부에서 이미 로그를 찍고 false를 반환
        return handleUnauthorized(exchange, "Token validation failed.");
      }
      log.info("✅ [Gateway] Token validation successful. Extracting claims...");

      // 클레임 추출
      Long userId = jwtTokenProvider.getUserIdFromJWT(token);
      String role = jwtTokenProvider.getRoleFromJWT(token);

      // userId가 null인 경우 처리 (클레임 이름 불일치 등)
      if (userId == null) {
        log.error("❌ [Gateway] Could not extract userId from token. Check claim names ('USERID' vs 'userId').");
        return handleUnauthorized(exchange, "Invalid token claims.");
      }

      log.info("✅ [Gateway] Claims extracted. UserId: {}, Role: {}", userId, role);

      // 새로운 헤더를 추가하여 다운스트림으로 요청 전달
      ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
              .header("X-User-Id", String.valueOf(userId))
              .header("X-User-Role", role)
              .build();

      log.info("✅ [Gateway] X-User-Id and X-User-Role headers added. Forwarding request to downstream.");

      return chain.filter(exchange.mutate().request(mutatedRequest).build());

    } catch (Exception e) {
      log.error("❌ [Gateway] An unexpected error occurred during token processing.", e);
      return handleUnauthorized(exchange, "Error processing token.");
    }
  }

  /**
   * 인증이 필요하지 않은 경로인지 확인
   */
  private boolean isPermitAllPath(String path) {
    boolean isPublicNewsPath = path.startsWith("/api/news") &&
                               !path.contains("/mypage") &&
                               !path.contains("/report") &&
                               !path.contains("/scrap");

    return path.startsWith("/api/users/signup")
            || path.startsWith("/api/auth/")
            || path.startsWith("/api/users/categories")
            || isPublicNewsPath
            || path.startsWith("/swagger-ui")
            || path.contains("api-docs");
  }

  /**
   * 401 Unauthorized 응답을 처리하는 헬퍼 메소드
   */
  private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    // 필요하다면 응답 본문에 에러 메시지를 추가할 수도 있습니다.
    return exchange.getResponse().setComplete();
  }

  @Override
  public int getOrder() {
    // 이 필터가 다른 필터들보다 먼저 실행되도록 순서를 높게 설정합니다.
    return -1;
  }
}