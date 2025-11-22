package org.example.ticketing.domain.reservation.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.event.repository.EventRepository;
import org.example.ticketing.domain.reservation.entity.Reservation;
import org.example.ticketing.domain.reservation.enums.Status;
import org.example.ticketing.domain.reservation.exception.InvalidHoldException;
import org.example.ticketing.domain.reservation.exception.SeatAlreadyHeldException;
import org.example.ticketing.domain.reservation.repository.ReservationRepository;
import org.example.ticketing.domain.seat.exception.SeatNotFoundException;
import org.example.ticketing.domain.seat.repository.SeatRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationService {
  private final ReservationHoldService holdService;
  private final SeatRepository seatRepository;
  private final ReservationRepository reservationRepository;

  public Mono<Void> confirmReservation(Long seatId, Long userId, String token) {
    return holdService.validateHold(seatId, userId, token)
            .flatMap(valid -> {
              if (!valid) {
                return Mono.error(new InvalidHoldException());
              }
              return Mono.empty();
            })
            .then(
                    seatRepository.findById(seatId)
                            .switchIfEmpty(Mono.error(new SeatNotFoundException()))
                            .flatMap(seat -> {
                              seat.setReserved(true);
                              return seatRepository.save(seat)
                                      .then(
                                              reservationRepository.save(
                                                      Reservation.builder()
                                                              .userId(userId)
                                                              .eventId(seat.getEventId())
                                                              .seatId(seatId)
                                                              .status(Status.CONFIRMED)
                                                              .reservedAt(LocalDateTime.now())
                                                              .build()
                                              )
                                      );
                            })
            )
            .then(
                    holdService.releaseHold(seatId)
            )
            .then();
  }
}
