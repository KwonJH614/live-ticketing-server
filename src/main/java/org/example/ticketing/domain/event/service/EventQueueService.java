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

  private final static String QUEUE_KEY = "event:queue:";
  private final static long ALLOWED_USER_COUNT = 1;

  public Mono<Long> registerQueue(Long eventId, Long userId) {
    String key = QUEUE_KEY + eventId;
    long timestamp = Instant.now().toEpochMilli();
    String userIdStr = String.valueOf(userId);

    return redis.opsForZSet()
        .add(key, userIdStr, timestamp)
        .then(getRank(eventId, userId));
  }

  public Mono<Long> getRank(Long eventId, Long userId) {
    String key = QUEUE_KEY + eventId;
    String userIdStr = String.valueOf(userId);

    return redis.opsForZSet()
        .rank(key, userIdStr)
        .map(rank -> rank + 1)
        .defaultIfEmpty(0L);
  }

  public Mono<QueueStatusDto> getQueueStatus(Long eventId, Long userId) {
    return getRank(eventId, userId)
        .map(rank -> new QueueStatusDto(rank, rank > 0 && rank <= ALLOWED_USER_COUNT));
  }

  public Mono<Boolean> deleteQueue(Long eventId, Long userId) {
    String key = QUEUE_KEY + eventId;
    String userIdStr = String.valueOf(userId);

    return redis.opsForZSet()
        .remove(key, userIdStr)
        .map(removed -> removed > 0);
  }
}