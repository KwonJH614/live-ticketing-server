package org.example.ticketing.domain.event.repository;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.event.entity.Event;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
@RequiredArgsConstructor
public class EventCustomRepository {

  private final DatabaseClient client;

  public Flux<Event> findAllPaged(int page, int size) {
    int offset = page * size;
    String sql = """
            SELECT * FROM events
            ORDER BY created_at DESC
            LIMIT :size OFFSET :offset
            """;

    return client.sql(sql)
            .bind("size", size)
            .bind("offset", offset)
            .map((row, meta) -> Event.builder()
                    .id(row.get("id", Long.class))
                    .title(row.get("title", String.class))
                    .description(row.get("description", String.class))
                    .startTime(row.get("start_time", LocalDateTime.class))
                    .endTime(row.get("end_time", LocalDateTime.class))
                    .venue(row.get("venue", String.class))
                    .price(row.get("price", Integer.class))
                    .createdAt(row.get("created_at", LocalDateTime.class))
                    .build()
            )
            .all();
  }

  public Mono<Long> countAll() {
    return client.sql("SELECT COUNT(*) AS cnt FROM events")
            .map((row, meta) -> row.get("cnt", Long.class))
            .one();
  }
}
