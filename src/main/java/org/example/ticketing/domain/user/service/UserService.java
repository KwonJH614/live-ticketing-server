package org.example.ticketing.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.user.dto.LoginRequestDto;
import org.example.ticketing.domain.user.dto.RegisterRequest;
import org.example.ticketing.domain.user.dto.RegisterResponse;
import org.example.ticketing.domain.user.dto.TokenResponse;
import org.example.ticketing.domain.user.entity.User;
import org.example.ticketing.domain.user.enums.Role;
import org.example.ticketing.domain.user.exception.DuplicateEmailException;
import org.example.ticketing.domain.user.exception.DuplicateUsernameException;
import org.example.ticketing.domain.user.exception.InvalidPasswordException;
import org.example.ticketing.domain.user.exception.InvalidVerificationCodeException;
import org.example.ticketing.domain.user.repository.UserRepository;
import org.example.ticketing.global.exception.UserNotFoundException;
import org.example.ticketing.security.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final EmailVerificationService emailVerificationService;
  private final BCryptPasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  public Mono<RegisterResponse> register(RegisterRequest dto) {
    return verifyCode(dto.email(), dto.verificationCode())
        .then(validateEmail(dto.email()))
        .then(validateUsername(dto.username()))
        .then(createUser(dto));
  }

  public Mono<TokenResponse> login(LoginRequestDto dto) {
    return userRepository.findByUsername(dto.username())
        .switchIfEmpty(Mono.error(new UserNotFoundException()))
        .flatMap(user -> Mono.fromCallable(() -> passwordEncoder.matches(dto.password(), user.getPassword()))
            .subscribeOn(Schedulers.boundedElastic())
            .flatMap(matches -> matches
                ? Mono.just(createTokenResponse(user))
                : Mono.error(new InvalidPasswordException()))
        );
  }

  private Mono<Void> verifyCode(String email, String code) {
    return emailVerificationService.verifyCode(email, code)
        .filter(isValid -> isValid)
        .switchIfEmpty(Mono.error(new InvalidVerificationCodeException()))
        .then();
  }

  private Mono<Void> validateEmail(String email) {
    return userRepository.existsByEmail(email)
        .filter(exists -> !exists)
        .switchIfEmpty(Mono.error(new DuplicateEmailException(email)))
        .then();
  }

  private Mono<Void> validateUsername(String username) {
    return userRepository.existsByUsername(username)
        .filter(exists -> !exists)
        .switchIfEmpty(Mono.error(new DuplicateUsernameException(username)))
        .then();
  }

  private Mono<RegisterResponse> createUser(RegisterRequest dto) {
    return Mono.fromCallable(() -> passwordEncoder.encode(dto.password()))
        .subscribeOn(Schedulers.boundedElastic())
        .map(encodedPassword -> User.builder()
            .username(dto.username())
            .email(dto.email())
            .password(encodedPassword)
            .role(Role.USER)
            .createdAt(LocalDateTime.now())
            .build())
        .flatMap(userRepository::save)
        .map(saved -> new RegisterResponse(saved.getUsername(), "Register completed"));
  }

  private TokenResponse createTokenResponse(User user) {
    return new TokenResponse(
        jwtUtil.generateToken(
            user.getId(),
            user.getUsername(),
            user.getRole().name()
        )
    );
  }
}
