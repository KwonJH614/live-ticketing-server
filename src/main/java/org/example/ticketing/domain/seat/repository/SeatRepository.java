package org.example.ticketing.domain.seat.repository;

import org.example.ticketing.domain.seat.entity.Seat;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SeatRepository extends ReactiveCrudRepository<Seat, Long> {
  Flux<Seat> findAllByEventIdOrderById(Long eventId);
  @Modifying
  @Query("UPDATE seats SET is_reserved = true WHERE id = :seatId AND is_reserved = false")
  Mono<Integer> reserveIfAvailable(Long seatId);
  @Modifying
  @Query("UPDATE seats SET is_reserved = false WHERE id = :seatId")
  Mono<Integer> releaseSeat(Long seatId);
}
