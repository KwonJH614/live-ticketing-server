package org.example.ticketing.domain.reservation.repository;

import org.example.ticketing.domain.reservation.entity.Reservation;
import org.example.ticketing.domain.reservation.enums.Status;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

public interface ReservationRepository extends ReactiveCrudRepository<Reservation, Long> {
  Mono<Reservation> findByIdAndUserId(Long id, Long userId);

  @Modifying
  @Query("UPDATE reservations SET status = :status WHERE id = :id")
  Mono<Integer> updateStatus(Long id, Status status);

  @Modifying
  @Query("UPDATE reservations SET status = :newStatus WHERE id = :id AND status = :expectedStatus")
  Mono<Integer> compareAndUpdateStatus(Long id, Status expectedStatus, Status newStatus);

  @Modifying
  @Query("UPDATE reservations SET payment_id = :paymentId, status = :status WHERE id = :id")
  Mono<Integer> updatePaymentAndStatus(Long id, Long paymentId, Status status);
}
