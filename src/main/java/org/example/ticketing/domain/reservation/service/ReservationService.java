package org.example.ticketing.domain.reservation.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.payment.entity.Payment;
import org.example.ticketing.domain.payment.enums.PaymentStatus;
import org.example.ticketing.domain.payment.repository.PaymentRepository;
import org.example.ticketing.domain.payment.service.TossPaymentService;
import org.example.ticketing.domain.reservation.dto.ReservationListDto;
import org.example.ticketing.domain.reservation.entity.Reservation;
import org.example.ticketing.domain.reservation.enums.Status;
import org.example.ticketing.domain.reservation.exception.InvalidHoldException;
import org.example.ticketing.domain.reservation.repository.ReservationCustomRepository;
import org.example.ticketing.domain.reservation.repository.ReservationRepository;
import org.example.ticketing.domain.seat.exception.SeatNotFoundException;
import org.example.ticketing.domain.seat.repository.SeatRepository;
import org.example.ticketing.domain.user.repository.UserRepository;
import org.example.ticketing.global.dto.PageResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservationService {
  private final ReservationHoldService holdService;
  private final TossPaymentService paymentService;
  private final SeatRepository seatRepository;
  private final ReservationRepository reservationRepository;
  private final PaymentRepository paymentRepository;
  private final ReservationCustomRepository reservationCustomRepository;
  private final TransactionalOperator txOperator;

  public Mono<Void> confirmReservation(Long seatId, Long userId, String token, String paymentKey, Long amount, String orderId) {
    return holdService.validateHold(seatId, userId, token)
        .flatMap(valid -> {
          if (!valid) {
            return Mono.error(new InvalidHoldException());
          }
          return Mono.empty();
        })
        .then(paymentService.approvePayment(paymentKey, orderId, amount))
        .flatMap(approval ->
            paymentRepository.save(Payment.builder()
                .orderId(approval.getOrderId())
                .paymentKey(approval.getPaymentKey())
                .amount(approval.getAmount())
                .status(PaymentStatus.SUCCESS)
                .createdAt(LocalDateTime.now())
                .build())
            )
        .flatMap(savedPayment ->
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
                                  .paymentId(savedPayment.getId())
                                  .status(Status.CONFIRMED)
                                  .reservedAt(LocalDateTime.now())
                                  .build()
                          )
                      );
                })
        )
        .as(txOperator::transactional)
        .then(
            holdService.releaseHold(seatId, userId, token)
        )
        .then();
  }

  public Mono<PageResponse<ReservationListDto>> getReservations(Long userId, int page, int size) {
    return Mono.zip(
        reservationCustomRepository.countByUserId(userId),
        reservationCustomRepository.findAllPagedByUserId(userId, page, size).collectList()
    ).map(tuple -> {
      long totalCount = tuple.getT1();
      List<ReservationListDto> reservations = tuple.getT2();

      return new PageResponse<>(reservations, page, size, totalCount);
    });
  }
}
