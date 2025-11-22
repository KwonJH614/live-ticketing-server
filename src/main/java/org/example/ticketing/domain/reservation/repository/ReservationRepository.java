package org.example.ticketing.domain.reservation.repository;

import org.example.ticketing.domain.reservation.entity.Reservation;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface ReservationRepository extends ReactiveCrudRepository<Reservation, Long> {
}
