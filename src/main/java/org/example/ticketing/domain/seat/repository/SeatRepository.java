package org.example.ticketing.domain.seat.repository;

import org.example.ticketing.domain.seat.entity.Seat;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SeatRepository extends ReactiveCrudRepository<Seat, Long> {
  Flux<Seat> findAllByEventId(Long eventId);
}
