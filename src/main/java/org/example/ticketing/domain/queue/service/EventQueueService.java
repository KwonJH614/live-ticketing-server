package org.example.ticketing.domain.queue.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.queue.dto.QueueStatusDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class EventQueueService {

  private final ReactiveStringRedisTemplate redis;

  private static final String QUEUE_KEY = "event:queue:";
  private static final String ACCESS_KEY = "event:access:";
  private static final long ALLOWED_USER_COUNT = 50;
  @Value("${spring.queue-ttl}")
  private Duration QUEUE_TTL;
  @Value("${spring.access-ttl}")
  private Duration ACCESS_TTL;

  private String queueKey(Long eventId) {
    return QUEUE_KEY + eventId;
  }

  private String accessKey(Long eventId, Long userId) {
    return ACCESS_KEY + eventId + ":" + userId;
  }

  public Mono<Boolean> grantAccess(Long eventId, Long userId) {
    return redis.opsForValue()
        .set(accessKey(eventId, userId), "granted", ACCESS_TTL);
  }

  public Mono<Boolean> hasAccess(Long eventId, Long userId) {
    return redis.hasKey(accessKey(eventId, userId));
  }

  public Mono<Boolean> revokeAccess(Long eventId, Long userId) {
    return redis.delete(accessKey(eventId, userId))
        .map(count -> count > 0);
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