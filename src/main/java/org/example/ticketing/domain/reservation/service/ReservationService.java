package org.example.ticketing.domain.reservation.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.payment.dto.PaymentApprovalResponseDto;
import org.example.ticketing.domain.payment.entity.Payment;
import org.example.ticketing.domain.payment.enums.PaymentStatus;
import org.example.ticketing.domain.payment.repository.PaymentRepository;
import org.example.ticketing.domain.payment.service.TossPaymentService;
import org.example.ticketing.domain.reservation.dto.ReservationListDto;
import org.example.ticketing.domain.reservation.entity.Reservation;
import org.example.ticketing.domain.reservation.enums.Status;
import org.example.ticketing.domain.reservation.exception.InvalidHoldException;
import org.example.ticketing.domain.reservation.exception.SeatAlreadyReservedException;
import org.example.ticketing.domain.reservation.repository.ReservationCustomRepository;
import org.example.ticketing.domain.reservation.repository.ReservationRepository;
import org.example.ticketing.domain.seat.entity.Seat;
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

  public Mono<Void> confirmReservation(Long seatId, Long userId, String token,
                                       String paymentKey, Long amount, String orderId) {

    return validateHold(seatId, userId, token)
        .then(processPayment(paymentKey, orderId, amount))
        .flatMap(payment -> reserve(seatId, userId, payment))
        .as(txOperator::transactional)
        .flatMap(res -> releaseHold(seatId, userId, token))
        .then();
  }

  private Mono<Void> validateHold(Long seatId, Long userId, String token) {
    return holdService.validateHold(seatId, userId, token)
        .filter(Boolean::booleanValue)
        .switchIfEmpty(Mono.error(new InvalidHoldException()))
        .then();
  }

  private Mono<Void> releaseHold(Long seatId, Long userId, String token) {
    return holdService.releaseHold(seatId, userId, token).then();
  }

  private Mono<Payment> processPayment(String paymentKey, String orderId, Long amount) {
    return paymentService.approvePayment(paymentKey, orderId, amount)
        .flatMap(this::savePayment);
  }

  private Mono<Payment> savePayment(PaymentApprovalResponseDto approval) {
    return paymentRepository.save(Payment.builder()
        .orderId(approval.orderId())
        .paymentKey(approval.paymentKey())
        .amount(approval.amount())
        .status(PaymentStatus.SUCCESS)
        .createdAt(LocalDateTime.now())
        .build());
  }

  private Mono<Reservation> reserve(Long seatId, Long userId, Payment payment) {
    return seatRepository.findById(seatId)
        .switchIfEmpty(Mono.error(new SeatNotFoundException()))
        .flatMap(this::validateSeatAvailable)
        .flatMap(seat ->
            reserveSeat(seat)
                .then(createReservation(seat, userId, payment))
        );
  }

  private Mono<Seat> validateSeatAvailable(Seat seat) {
    return seat.isReserved()
        ? Mono.error(new SeatAlreadyReservedException())
        : Mono.just(seat);
  }

  private Mono<Seat> reserveSeat(Seat seat) {
    seat.setReserved(true);
    return seatRepository.save(seat);
  }

  private Mono<Reservation> createReservation(Seat seat, Long userId, Payment payment) {
    return reservationRepository.save(
        Reservation.builder()
            .userId(userId)
            .eventId(seat.getEventId())
            .seatId(seat.getId())
            .paymentId(payment.getId())
            .status(Status.CONFIRMED)
            .reservedAt(LocalDateTime.now())
            .build()
    );
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
