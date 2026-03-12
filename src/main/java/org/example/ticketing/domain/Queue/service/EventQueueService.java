package org.example.ticketing.domain.Queue.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.Queue.dto.QueueStatusDto;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class EventQueueService {

  private final ReactiveStringRedisTemplate redis;

  private static final String QUEUE_KEY = "event:queue:";
  private static final long ALLOWED_USER_COUNT = 100;
  private static final Duration QUEUE_TTL = Duration.ofHours(2);

  private String queueKey(Long eventId) {
    return QUEUE_KEY + eventId;
  }

  public Mono<Long> registerQueue(Long eventId, Long userId) {
    String key = queueKey(eventId);
    String userIdStr = String.valueOf(userId);
    double score = System.nanoTime();

    return redis.opsForZSet()
        .rank(key, userIdStr)
        .switchIfEmpty(
            redis.opsForZSet()
                .add(key, userIdStr, score)
                .then(redis.expire(key, QUEUE_TTL))
                .then(redis.opsForZSet().rank(key, userIdStr))
        )
        .map(rank -> rank + 1)
        .defaultIfEmpty(0L);
  }

  public Mono<Long> getRank(Long eventId, Long userId) {
    String key = queueKey(eventId);
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

  public Mono<Boolean> validateEntry(Long eventId, Long userId) {
    return getRank(eventId, userId)
        .map(rank -> rank > 0 && rank <= ALLOWED_USER_COUNT);
  }

  public Mono<Boolean> deleteQueue(Long eventId, Long userId) {
    String key = queueKey(eventId);
    String userIdStr = String.valueOf(userId);

    return redis.opsForZSet()
        .remove(key, userIdStr)
        .map(removed -> removed > 0);
  }
}