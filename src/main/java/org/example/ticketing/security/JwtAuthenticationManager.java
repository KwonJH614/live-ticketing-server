//package org.example.ticketing.security;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.security.authentication.BadCredentialsException;
//import org.springframework.security.authentication.ReactiveAuthenticationManager;
//import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.GrantedAuthority;
//import org.springframework.security.core.authority.SimpleGrantedAuthority;
//import org.springframework.stereotype.Component;
//import reactor.core.publisher.Mono;
//
//import java.util.List;
//
//@Component
//@RequiredArgsConstructor
//public class JwtAuthenticationManager implements ReactiveAuthenticationManager {
//  private final JwtUtil jwtUtil;
//
//  @Override
//  public Mono<Authentication> authenticate(Authentication authentication) {
//    String token = authentication.getCredentials().toString();
//
//    try {
//      String email = jwtUtil.getEmail(token);
//      String role = jwtUtil.getRole(token);
//
//      List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
//      return Mono.just(new UsernamePasswordAuthenticationToken(email, null, authorities));
//    } catch (Exception e) {
//      return Mono.error(new BadCredentialsException("Invalid token"));
//    }
//  }
//
//}
