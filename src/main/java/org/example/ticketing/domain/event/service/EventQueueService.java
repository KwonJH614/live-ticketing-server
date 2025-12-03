package org.example.ticketing.domain.event.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.event.dto.QueueStatusDto;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EventQueueService {
  private final ReactiveStringRedisTemplate redis;

  private final static String QUEUE_KEY = "event:queue";
  private final static long ALLOWED_USER_COUNT = 1;

  public Mono<Long> registerQueue(Long eventId, String username) {
    String key = QUEUE_KEY + eventId;
    long timestamp = Instant.now().toEpochMilli();

    return redis.opsForZSet()
        .add(key, username, timestamp)
        .flatMap(added -> {
          if (Boolean.FALSE.equals(added)) {
            return getRank(eventId, username);
          }
          return getRank(eventId, username);
        });
  }

  public Mono<Long> getRank(Long eventId, String username) {
    String key = QUEUE_KEY + eventId;

    return redis.opsForZSet()
        .rank(key, username)
        .map(rank -> rank + 1)
        .defaultIfEmpty(0L);
  }

  public Mono<QueueStatusDto> getQueueStatus(Long eventId, String username) {
    String key = QUEUE_KEY + eventId;

    return getRank(eventId, username)
        .map(rank -> new QueueStatusDto(rank, rank > 0 && rank <= ALLOWED_USER_COUNT));
  }

  public Mono<Boolean> deleteQueue(Long eventId, String username) {
    String key = QUEUE_KEY + eventId;

    return redis.opsForZSet()
        .remove(key, username)
        .map(removed -> removed > 0);
  }
}
