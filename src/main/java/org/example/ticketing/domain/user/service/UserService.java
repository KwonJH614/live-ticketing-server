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
          return userRepository.existsByEmail(dto.getEmail())
              .flatMap(emailExists -> {
                if (emailExists) {
                  return Mono.error(new DuplicateEmailException(dto.getEmail()));
                }
                return userRepository.existsByUsername(dto.getUsername())
                    .flatMap(usernameExists -> {
                      if (usernameExists) {
                        return Mono.error(new DuplicateUsernameException(dto.getUsername()));
                      }
                      User user = User.builder()
                          .username(dto.getUsername())
                          .email(dto.getEmail())
                          .password(passwordEncoder.encode(dto.getPassword()))
                          .role(Role.USER)
                          .createdAt(LocalDateTime.now())
                          .build();

                      return userRepository.save(user)
                          .map(saved -> new RegisterResponse(saved.getUsername(), "회원가입 완료"));
                    });
              });
        });
  }

  public Mono<TokenResponse> login(LoginRequestDto dto) {
    return userRepository.findByUsername(dto.getUsername())
        .switchIfEmpty(Mono.error(new UserNotFoundException()))
        .flatMap(user ->
            passwordEncoder.matches(dto.getPassword(), user.getPassword())
                ? Mono.just(
                new TokenResponse(
                    jwtUtil.generateToken(
                        user.getId(),
                        user.getUsername(),
                        user.getRole().name()
                    )
                )
            )
                : Mono.error(new InvalidPasswordException())
        );
  }

}
