package org.example.ticketing.domain.user.repository;

import jakarta.validation.constraints.NotBlank;
import org.example.ticketing.domain.user.dto.RegisterResponse;
import org.example.ticketing.domain.user.entity.User;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface UserRepository extends ReactiveCrudRepository<User, Long> {
  Mono<User> findByEmail(String email);
  Mono<Boolean> existsByEmail(String email);
}