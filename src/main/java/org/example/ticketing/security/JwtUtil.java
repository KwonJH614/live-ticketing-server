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

  /**
   * Create a signed JSON Web Token containing the user's id, username, and role.
   *
   * The token is signed with the configured HMAC SHA-256 secret and expires after the configured expiration interval.
   *
   * @param userId  the user's unique identifier; stored in the token as the `userId` claim
   * @param username the user's username; set as the token subject
   * @param role    the user's role; stored in the token as the `role` claim
   * @return        the compact serialized JWT string
   */
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

  /**
   * Parse the provided JWT and extract its claims using the component's signing key.
   *
   * @param token the JWT compact string to parse
   * @return the parsed JWT claims
   */
  private Claims getClaims(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  /**
   * Extracts the user ID claim from the given JWT.
   *
   * @param token the JWT string containing the claims
   * @return the `userId` claim as a `Long`, or `null` if the claim is not present
   */
  public Long getUserId(String token) {
    return getClaims(token).get("userId", Long.class);
  }

  /**
   * Extracts the username (JWT subject) from the given token.
   *
   * @param token the JWT string to parse
   * @return the subject (username) contained in the token, or null if absent
   */
  public String getUsername(String token) {
    return getClaims(token).getSubject();
  }

  /**
   * Extracts the user's role claim from the given JWT.
   *
   * @param token the JWT string to parse
   * @return the role claim value from the token, or `null` if the claim is not present
   */
  public String getRole(String token) {
    return getClaims(token).get("role", String.class);
  }

  /**
   * Checks whether a JWT's signature and structure are valid using the configured secret.
   *
   * @param token the JWT string to validate
   * @return `true` if the token is valid and its signature matches the configured secret, `false` otherwise
   */
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