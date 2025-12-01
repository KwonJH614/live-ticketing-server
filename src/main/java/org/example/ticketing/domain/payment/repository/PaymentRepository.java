package org.example.ticketing.domain.payment.repository;

import org.example.ticketing.domain.payment.entity.Payment;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface PaymentRepository extends ReactiveCrudRepository<Payment, Long> {
}
