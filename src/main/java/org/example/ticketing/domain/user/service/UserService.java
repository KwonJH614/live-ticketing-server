package org.example.ticketing.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.common.exception.DuplicateUserException;
import org.example.ticketing.common.exception.InvalidPasswordException;
import org.example.ticketing.common.exception.InvalidVerificationCodeException;
import org.example.ticketing.common.exception.UserNotFoundException;
import org.example.ticketing.domain.user.dto.LoginRequestDto;
import org.example.ticketing.domain.user.dto.RegisterResponse;
import org.example.ticketing.domain.user.dto.TokenResponse;
import org.example.ticketing.domain.user.dto.RegisterRequest;
import org.example.ticketing.domain.user.entity.User;
import org.example.ticketing.domain.user.enums.Role;
import org.example.ticketing.domain.user.repository.UserRepository;
import org.example.ticketing.security.JwtUtil;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final EmailVerificationService emailVerificationService;
  private final BCryptPasswordEncoder passwordEncoder;
  private final JwtUtil jwtUtil;

  public Mono<RegisterResponse> register(RegisterRequest dto) {
    return emailVerificationService.verifyCode(dto.getEmail(), dto.getVerificationCode())
            .flatMap(isValid -> {
              if (!isValid) {
                return Mono.error(new InvalidVerificationCodeException());
              }

              return userRepository.existsByUsername(dto.getUsername())
                      .flatMap(exists -> {
                        if (exists) {
                          return Mono.error(new DuplicateUserException(dto.getUsername()));
                        }

                        return userRepository.save(
                                User.builder()
                                        .username(dto.getUsername())
                                        .email(dto.getEmail())
                                        .password(passwordEncoder.encode(dto.getPassword()))
                                        .role(Role.USER)
                                        .createdAt(LocalDateTime.now())
                                        .build()
                        ).map(saved -> new RegisterResponse(saved.getEmail(), "회원가입 완료"));
                      });
            });
  }

  public Mono<TokenResponse> login(LoginRequestDto dto) {
    return userRepository.findByEmail(dto.getEmail())
            .switchIfEmpty(Mono.error(new UserNotFoundException(dto.getEmail())))
            .flatMap(user -> passwordEncoder.matches(dto.getPassword(), user.getPassword())
                    ? Mono.just(new TokenResponse(jwtUtil.generateToken(dto.getEmail(), Role.USER.name())))
                    : Mono.error(new InvalidPasswordException()));
  }

}
