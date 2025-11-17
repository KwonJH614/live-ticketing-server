package org.example.ticketing.domain.seat.repository;

import org.example.ticketing.domain.seat.entity.Seat;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface SeatRepository extends ReactiveCrudRepository<Seat, Long> {
}
