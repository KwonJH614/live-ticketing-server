package org.example.ticketing.domain.seat.service;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.event.dto.CreateEventRequestDto;
import org.example.ticketing.domain.event.entity.Event;
import org.example.ticketing.domain.seat.entity.Seat;
import org.example.ticketing.domain.seat.repository.SeatRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatService {
  private final SeatRepository seatRepository;

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
}
