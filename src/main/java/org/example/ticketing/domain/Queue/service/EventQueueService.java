package org.example.ticketing.domain.Queue.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.Queue.dto.QueueStatusDto;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class EventQueueService {

  private final ReactiveStringRedisTemplate redis;

  private static final String QUEUE_KEY = "event:queue:";
  private static final long ALLOWED_USER_COUNT = 100;

  public Mono<Long> registerQueue(Long eventId, Long userId) {
    String key = QUEUE_KEY + eventId;
    String userKey = key + ":users";
    String userIdStr = String.valueOf(userId);
    String seqKey = key + ":seq";

    return redis.opsForSet()
        .add(userKey, userIdStr)
        .flatMap(added -> {
          if (added == 0) {
            return redis.opsForZSet()
                .rank(key, userIdStr)
                .map(rank -> rank + 1);
          }

          return redis.opsForValue()
              .increment(seqKey)
              .flatMap(seq -> redis.opsForZSet()
                  .add(key, userIdStr, seq)
                  .then(redis.opsForZSet()
                      .rank(key, userIdStr))
                      .map(rank -> rank + 1));
        });
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