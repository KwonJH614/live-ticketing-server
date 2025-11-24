package org.example.ticketing.domain.user.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.user.dto.LoginRequestDto;
import org.example.ticketing.domain.user.dto.RegisterRequest;
import org.example.ticketing.domain.user.dto.RegisterResponse;
import org.example.ticketing.domain.user.dto.TokenResponse;
import org.example.ticketing.domain.user.service.EmailVerificationService;
import org.example.ticketing.domain.user.service.UserService;
import org.example.ticketing.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class AuthController {
  private final UserService userService;
  private final EmailVerificationService emailVerificationService;

  @PostMapping("/verification-codes")
  public Mono<ResponseEntity<ApiResponse<String>>> sendVerificationCode(@RequestParam @NotBlank @Email String email) {
    return emailVerificationService.sendVerificationCode(email)
        .then(Mono.just(ResponseEntity.ok(
            ApiResponse.success("인증 코드가 이메일로 전송되었습니다")
        )));
  }

  @PostMapping("/register")
  public Mono<ResponseEntity<ApiResponse<RegisterResponse>>> register(
      @Valid @RequestBody RegisterRequest userDto) {
    return userService.register(userDto)
        .map(response -> ResponseEntity.ok(ApiResponse.success(response))
        );
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
