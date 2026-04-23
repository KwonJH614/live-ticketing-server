package org.example.ticketing.security;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.server.WebFilter;

import java.util.Collections;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

  private final JwtUtil jwtUtil;

  @Bean
  public BCryptPasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public SecurityWebFilterChain securityWebFilterChain() {
    return ServerHttpSecurity.http()
        .cors(cors -> {})
        .csrf(ServerHttpSecurity.CsrfSpec::disable)
        .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
        .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
        .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
        .authorizeExchange(exchanges -> exchanges
            .pathMatchers(
                "/swagger-ui.html",
                "/swagger-ui/**",
                "/v3/api-docs/**",
                "/webjars/**"
            ).permitAll()
            .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .pathMatchers(HttpMethod.GET, "/health").permitAll()
            .pathMatchers("/ws/**").permitAll()
            .pathMatchers(HttpMethod.POST, "/api/users/**").permitAll()
            .pathMatchers(HttpMethod.GET, "/api/events/**").permitAll()
            .pathMatchers(HttpMethod.GET, "/api/seat/**").permitAll()
            .pathMatchers(HttpMethod.POST, "/api/events").hasRole("ADMIN")
            .pathMatchers(HttpMethod.PUT, "/api/events/**").hasRole("ADMIN")
            .pathMatchers(HttpMethod.DELETE, "/api/events/**").hasRole("ADMIN")
            .anyExchange().authenticated()
        )
        .addFilterBefore(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
        .build();
  }

  @Bean
  public WebFilter jwtAuthenticationFilter() {
    return (exchange, chain) -> {
      String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);

        try {
          Claims claims = jwtUtil.getClaims(token);
          String username = claims.getSubject();
          String role = claims.get("role", String.class);
          Long userId = claims.get("userId", Long.class);

          if (username != null && !username.isEmpty() && role != null && !role.isEmpty()) {
            CustomUserPrincipal principal = new CustomUserPrincipal(userId, username);

            AbstractAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                principal, null,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
            );

            return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
          }
        } catch (Exception e) {
          log.warn("JWT authentication failed: {}", e.getMessage());
        }
      }

      return chain.filter(exchange);
    };
  }
}
