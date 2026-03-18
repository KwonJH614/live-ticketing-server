package org.example.ticketing.domain.payment.repository;

import org.example.ticketing.domain.payment.entity.Payment;
import org.example.ticketing.domain.payment.enums.PaymentStatus;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface PaymentRepository extends ReactiveCrudRepository<Payment, Long> {
  @Modifying
  @Query("UPDATE payments SET status = :status WHERE id = :id")
  Mono<Integer> updateStatus(Long id, PaymentStatus status);
}
