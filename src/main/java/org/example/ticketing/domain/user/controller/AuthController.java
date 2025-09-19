package org.example.ticketing.domain.user.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ticketing.common.response.ApiResponse;
import org.example.ticketing.domain.user.dto.LoginRequestDto;
import org.example.ticketing.domain.user.dto.RegisterResponse;
import org.example.ticketing.domain.user.dto.TokenResponse;
import org.example.ticketing.domain.user.dto.UserDto;
import org.example.ticketing.domain.user.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class AuthController {
  private final UserService userService;

  @PostMapping("/register")
  public Mono<ResponseEntity<ApiResponse<RegisterResponse>>> register(
          @Valid @RequestBody UserDto userDto) {
    return userService.register(userDto)
            .map(response -> ResponseEntity.ok(ApiResponse.success(response)))
            .onErrorResume(e -> Mono.just(
                    ResponseEntity.badRequest().body(ApiResponse.fail(e.getMessage()))
            ));
  }

  @PostMapping("/login")
  public Mono<ResponseEntity<ApiResponse<TokenResponse>>> login(
          @Valid @RequestBody LoginRequestDto loginRequestDto) {
    return userService.login(loginRequestDto)
            .map(token -> ResponseEntity.ok(ApiResponse.success(token)))
            .onErrorResume(e -> Mono.just(
                    ResponseEntity.status(401)
                            .body(ApiResponse.fail(e.getMessage()))
            ));
  }
}
