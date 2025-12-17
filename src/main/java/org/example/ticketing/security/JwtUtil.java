package org.example.ticketing.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class JwtUtil {

  @Value("${spring.jwt.secret}")
  private String secret;

  @Value("${spring.expiration-ms}")
  private Long expirationMs;

  public String generateToken(Long userId, String username, String role) {
    return Jwts.builder()
        .setSubject(username)
        .claim("userId", userId)
        .claim("role", role)
        .setIssuedAt(new Date())
        .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
        .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
        .compact();
  }

  private Claims getClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  public Long getUserId(String token) {
    return getClaims(token).get("userId", Long.class);
  }

  public String getUsername(String token) {
    return getClaims(token).getSubject();
  }

  public String getRole(String token) {
    return getClaims(token).get("role", String.class);
  }

  public boolean validateToken(String token) {
    try {
      Jwts.parserBuilder()
          .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
          .build()
          .parseClaimsJws(token);
      return true;
    } catch (Exception e) {
      return false;
    }
  }
}
