package com.newnormallist.gatewayservice.filter;

import com.newnormallist.gatewayservice.jwt.GatewayJwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
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
    // isPermitAllPath에 exchange 객체를 직접 전달하도록 수정,
    if (isPermitAllPath(exchange)) {
      log.info("✅ [Gateway] PermitAll path, skipping token validation for: {}", exchange.getRequest().getURI().getPath());
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
        return handleUnauthorized(exchange, "Token validation failed.");
      }
      log.info("✅ [Gateway] Token validation successful. Extracting claims...");

      // 클레임 추출
      Long userId = jwtTokenProvider.getUserIdFromJWT(token);
      String role = jwtTokenProvider.getRoleFromJWT(token);

      if (userId == null) {
        log.error("❌ [Gateway] Could not extract userId from token. Check claim names.");
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
   * 인증이 필요하지 않은 경로인지 확인 (더 정교하게 수정된 버전)
   */
  private boolean isPermitAllPath(ServerWebExchange exchange) {
    String path = exchange.getRequest().getURI().getPath();
    HttpMethod method = exchange.getRequest().getMethod();

    // 인증 없이 항상 허용되는 경로들
    if (path.startsWith("/api/users/signup") ||
        path.startsWith("/api/auth/") ||
        path.startsWith("/api/users/categories")) {
      return true;
    }

    // 뉴스 API에 대한 특별 규칙
    if (path.startsWith("/api/news") && method == HttpMethod.GET) {
      if (path.equals("/api/news") || path.matches("/api/news/\\d+")) {
        return true;
      }
    }

    // 위 규칙에 해당하지 않는 모든 요청은 토큰 검증이 필요.
    return false;
  }

  /**
   * 401 Unauthorized 응답을 처리하는 헬퍼 메소드
   */
  private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    return exchange.getResponse().setComplete();
  }

  @Override
  public int getOrder() {
    return -1; // 이 필터가 다른 필터들보다 먼저 실행되도록 설정
  }
}