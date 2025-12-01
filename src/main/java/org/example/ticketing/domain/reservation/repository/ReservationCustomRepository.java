package org.example.ticketing.domain.reservation.repository;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.event.entity.Event;
import org.example.ticketing.domain.reservation.dto.ReservationListDto;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class ReservationCustomRepository {
  private final DatabaseClient client;

  public Flux<ReservationListDto> findAllPagedByUserId(Long userId, int page, int size) {
    int offset = page * size;
    String sql = """
        SELECT r.id as reservation_id, e.title as event_name, s.seat_number, r.status, r.reserved_at
        FROM reservations r
        JOIN events e ON r.event_id = e.id
        JOIN seats s ON r.seat_id = s.id
        WHERE r.user_id = :userId
        ORDER BY r.reserved_at DESC
        LIMIT :size OFFSET :offset
        """;

    return client.sql(sql)
        .bind("userId", userId)
        .bind("size", size)
        .bind("offset", offset)
        .map((row, meta) -> ReservationListDto.builder()
            .id(row.get("reservation_id", Long.class))
            .eventName(row.get("event_name", String.class))
            .seatNumber(row.get("seat_number", String.class))
            .status(row.get("status", String.class))
            .reservedAt(row.get("reserved_at", LocalDateTime.class))
            .build()
        )
        .all();
  }

  public Mono<Long> countByUserId(Long userId) {
    String sql = "SELECT COUNT(*) AS cnt FROM reservations WHERE user_id = $1";

    return client.sql(sql)
        .bind(0, userId)
        .map((row, meta) -> row.get("cnt", Long.class))
        .one();
  }
}
