package org.example.ticketing.domain.seat.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.event.dto.CreateEventRequestDto;
import org.example.ticketing.domain.event.entity.Event;
import org.example.ticketing.domain.event.exception.EventNotFoundException;
import org.example.ticketing.domain.event.repository.EventRepository;
import org.example.ticketing.domain.seat.dto.SeatListDto;
import org.example.ticketing.domain.seat.entity.Seat;
import org.example.ticketing.domain.seat.repository.SeatRepository;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {
  private final SeatRepository seatRepository;
  private final EventRepository eventRepository;
  private final ReactiveStringRedisTemplate redis;

  public Mono<Void> createSeatsForEvent(Event event, CreateEventRequestDto dto) {
    List<Seat> seats = generateSeats(event.getId(), dto.getRow(), dto.getColumn());
    return seatRepository.saveAll(seats).then();
  }

  private List<Seat> generateSeats(Long eventId, int row, int column) {
    List<Seat> seats = new ArrayList<>();
    char rowPrefix = 'A';

    for (int i = 0; i < row; i++) {
      for (int j = 0; j < column; j++) {
        seats.add(Seat.builder()
            .eventId(eventId)
            .seatNumber(rowPrefix + String.valueOf(j))
            .isReserved(false)
            .build());
      }

      rowPrefix++;
    }

    return seats;
  }

  public Mono<List<SeatListDto>> getSeats(Long eventId) {
    return eventRepository.findById(eventId)
        .switchIfEmpty(Mono.error(new EventNotFoundException()))
        .then(
            seatRepository.findAllByEventIdOrderById(eventId)
                .collectList()
                .map(
                    seats ->
                        seats.stream()
                            .map(seat -> SeatListDto.builder()
                                .id(seat.getId())
                                .seatNumber(seat.getSeatNumber())
                                .isReserved(seat.isReserved())
                                .build())
                            .toList()
                )
        );
  }

}
