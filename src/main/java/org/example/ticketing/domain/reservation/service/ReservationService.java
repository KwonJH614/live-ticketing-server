package org.example.ticketing.domain.reservation.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.payment.dto.PaymentApprovalResponseDto;
import org.example.ticketing.domain.payment.entity.Payment;
import org.example.ticketing.domain.payment.enums.PaymentStatus;
import org.example.ticketing.domain.payment.mapper.PaymentMapper;
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
  private final PaymentMapper paymentMapper;

  public Mono<Void> confirmReservation(Long seatId, Long userId, String token,
                                       String paymentKey, Long amount, String orderId) {

    return validateHold(seatId, userId, token)
        .then(approvePayment(paymentKey, orderId, amount))
        .flatMap(approval -> savePaymentAndReserve(seatId, userId, approval))
        .doFinally(signal -> releaseHold(seatId, userId, token).subscribe())
        .then();
  }

  private Mono<Void> validateHold(Long seatId, Long userId, String token) {
    return holdService.validateHold(seatId, userId, token)
        .flatMap(valid -> valid ? Mono.empty() : Mono.error(new InvalidHoldException()));
  }

  private Mono<PaymentApprovalResponseDto> approvePayment(String paymentKey, String orderId, Long amount) {
    return paymentService.approvePayment(paymentKey, orderId, amount);
  }

  private Mono<Void> savePaymentAndReserve(Long seatId, Long userId, PaymentApprovalResponseDto approval) {
    return txOperator.transactional(
        paymentRepository.save(paymentMapper.from(approval))
            .flatMap(payment -> reserveSeatAndCreateReservation(seatId, userId, payment))
    );
  }

  private Mono<Void> reserveSeatAndCreateReservation(Long seatId, Long userId, Payment payment) {
    return seatRepository.reserveIfAvailable(seatId)
        .filter(updated -> updated == 1)
        .switchIfEmpty(Mono.error(new SeatAlreadyReservedException()))
        .then(seatRepository.findById(seatId))
        .switchIfEmpty(Mono.error(new SeatNotFoundException()))
        .flatMap(seat -> createReservation(seat, userId, payment))
        .then();
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

  private Mono<Void> releaseHold(Long seatId, Long userId, String token) {
    return holdService.releaseHold(seatId, userId, token);
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
