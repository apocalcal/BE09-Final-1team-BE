package com.newnormallist.gatewayservice.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Slf4j
@Component
public class GatewayJwtTokenProvider {

  @Value("${jwt.secret}")
  private String jwtSecret;

  private SecretKey secretKey;

  @PostConstruct
  public void init() {
    byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
    secretKey = Keys.hmacShaKeyFor(keyBytes);
  }

  /**
   * JWT 토큰의 유효성을 검증합니다.
   * @param token 검증할 JWT 토큰
   * @return 유효하면 true, 아니면 false
   */
  public boolean validateToken(String token) {
    try {
      // 서명 키를 사용하여 토큰을 파싱하고 유효성을 검사합니다.
      Jwts.parser()
              .setSigningKey(secretKey)
              .build()
              .parseClaimsJws(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      // JwtException은 SecurityException, MalformedJwtException, ExpiredJwtException, UnsupportedJwtException을 포함합니다.
      log.error("Invalid JWT Token: {}", e.getMessage());
      return false;
    }
  }

  /**
   * 토큰에서 사용자 ID (userId)를 추출합니다.
   * @param token JWT 토큰
   * @return 사용자 ID
   */
  public Long getUserIdFromJWT(String token) {
    Claims claims = Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getBody();
    return claims.get("userId", Long.class);
  }

  /**
   * 토큰에서 사용자 역할(Role)을 추출합니다.
   * @param token JWT 토큰
   * @return 사용자 역할
   */
  public String getRoleFromJWT(String token) {
    Claims claims = Jwts.parser()
            .setSigningKey(secretKey)
            .build()
            .parseClaimsJws(token)
            .getBody();
    return claims.get("role", String.class);
  }
}