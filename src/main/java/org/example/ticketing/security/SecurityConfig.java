package org.example.ticketing.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;

import java.util.Collections;

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
    ServerHttpSecurity http = ServerHttpSecurity.http();

    return http
            .csrf(ServerHttpSecurity.CsrfSpec::disable)
            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            .authorizeExchange(exchanges -> exchanges
                    .pathMatchers("/api/users/register", "/api/users/login").permitAll()
                    .anyExchange().authenticated()
            )
            .addFilterAt(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
            .build();
  }

  @Bean
  public WebFilter jwtAuthenticationFilter() {
    return (ServerWebExchange exchange, WebFilterChain chain) -> {
      String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

      if (authHeader != null && authHeader.startsWith("Bearer ")) {
        String token = authHeader.substring(7);
        String email = jwtUtil.getEmail(token);

        AbstractAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                email, null,
                Collections.singletonList(new SimpleGrantedAuthority("USER"))
        );
        exchange.getAttributes().put("auth", auth);
      }

      return chain.filter(exchange);
    };
  }
}
