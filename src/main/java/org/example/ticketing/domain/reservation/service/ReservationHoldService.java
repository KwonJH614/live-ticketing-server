package org.example.ticketing.domain.reservation.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.queue.service.EventQueueService;
import org.example.ticketing.domain.reservation.dto.HoldResponseDto;
import org.example.ticketing.domain.reservation.exception.QueueAccessRequiredException;
import org.example.ticketing.domain.reservation.exception.SeatAlreadyHeldException;
import org.example.ticketing.domain.reservation.exception.SeatAlreadyReservedException;
import org.example.ticketing.domain.seat.repository.SeatRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReservationHoldService {
  private final ReactiveStringRedisTemplate redis;
  private final SeatRepository seatRepository;
  private final EventQueueService eventQueueService;

  @Value("${reservation.hold-ttl}")
  private Duration holdTtl;

  public Mono<HoldResponseDto> holdSeat(Long seatId, Long userId) {
    String key = "seat:%d:hold".formatted(seatId);
    String token = UUID.randomUUID().toString();
    String value = "%s:%d:%d".formatted(token, userId, seatId);

    return seatRepository.findById(seatId)
        .flatMap(seat -> {
          if (seat.isReserved()) {
            return Mono.error(new SeatAlreadyReservedException());
          }

          return eventQueueService.hasAccess(seat.getEventId(), userId)
              .flatMap(hasAccess -> {
                if (!hasAccess) {
                  return Mono.error(new QueueAccessRequiredException());
                }
                return Mono.just(seat);
              });
        })
        .then(
            redis.opsForValue()
                .setIfAbsent(key, value, holdTtl)
                .flatMap(success -> {
                  if (!success) {
                    return Mono.error(new SeatAlreadyHeldException());
                  }
                  return Mono.just(token);
                })
                .map(t -> HoldResponseDto.builder()
                    .seatId(seatId)
                    .token(t)
                    .build())
        );
  }

  public Mono<Boolean> validateHold(Long seatId, Long userId, String token) {
    String key = "seat:%d:hold".formatted(seatId);
    return redis.opsForValue()
        .get(key)
        .map(value -> {
          if (value == null) return false;
          String[] parts = value.split(":");
          if (parts.length < 3) return false;
          String storedToken = parts[0];
          String storedUserId = parts[1];
          return storedToken.equals(token) && storedUserId.equals(String.valueOf(userId));
        })
        .defaultIfEmpty(false);
  }

  public Mono<Void> releaseHold(Long seatId, Long userId, String token) {
    String key = "seat:%d:hold".formatted(seatId);

    return validateHold(seatId, userId, token)
        .flatMap(isValid -> {
          if (isValid) {
            return redis.delete(key).then();
          }
          return Mono.empty();
        });
  }
}
