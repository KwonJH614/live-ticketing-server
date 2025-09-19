//package org.example.ticketing.security;
//
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.http.HttpHeaders;
//import org.springframework.http.HttpMethod;
//import org.springframework.security.authentication.AbstractAuthenticationToken;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
//import org.springframework.security.config.web.server.ServerHttpSecurity;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.security.core.context.ReactiveSecurityContextHolder;
//import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
//import org.springframework.security.web.server.SecurityWebFilterChain;
//import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository;
//import org.springframework.web.server.ServerWebExchange;
//import org.springframework.web.server.WebFilter;
//import reactor.core.publisher.Mono;
//
//import java.util.Collections;
//
//@Slf4j
//@Configuration
//@RequiredArgsConstructor
//public class SecurityConfig {
//
//  private final JwtUtil jwtUtil;
//
//  @Bean
//  public BCryptPasswordEncoder passwordEncoder() {
//    return new BCryptPasswordEncoder();
//  }
//
//  @Bean
//  public SecurityWebFilterChain securityWebFilterChain() {
//    ServerHttpSecurity http = ServerHttpSecurity.http();
//
//    return http
//            .csrf(ServerHttpSecurity.CsrfSpec::disable)
//            .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
//            .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
//            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
//            .authorizeExchange(exchanges -> exchanges
//                    .pathMatchers(HttpMethod.POST, "/api/users/register").permitAll()
//                    .pathMatchers(HttpMethod.POST, "/api/users/login").permitAll()
//                    .pathMatchers("/health").permitAll()
//                    .anyExchange().authenticated()
//            )
//            .addFilterBefore(jwtAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
//            .build();
//  }
//
//  @Bean
//  public WebFilter jwtAuthenticationFilter() {
//    return (exchange, chain) -> {
//      ServerWebExchange currentExchange = exchange;
//      String path = exchange.getRequest().getPath().value();
//
//      if (isPublicPath(path)) {
//        log.debug("Public path accessed: {}", path);
//        return chain.filter(exchange);
//      }
//
//      String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
//
//      if (authHeader != null && authHeader.startsWith("Bearer ")) {
//        String token = authHeader.substring(7);
//
//        try {
//          if (jwtUtil.validateToken(token)) {
//            String email = jwtUtil.getEmail(token);
//
//            if (email != null && !email.isEmpty()) {
//              AbstractAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
//                      email,
//                      null,
//                      Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
//              );
//
//              log.debug("JWT authentication successful for user: {}", email);
//
//              return chain.filter(exchange)
//                      .contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
//            }
//          }
//        } catch (Exception e) {
//          log.warn("JWT authentication failed: {}", e.getMessage());
//        }
//      }
//
//      return chain.filter(exchange);
//    };
//  }
//
//  private boolean isPublicPath(String path) {
//    return path.startsWith("/api/users/register") ||
//            path.startsWith("/api/users/login") ||
//            path.equals("/health");
//  }
//}