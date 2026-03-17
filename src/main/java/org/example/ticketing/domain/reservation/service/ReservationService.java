package org.example.ticketing.domain.reservation.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.payment.dto.PaymentApprovalResponseDto;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
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
        .then(reserveSeatAndCreatePending(seatId, userId))
        .flatMap(reservation ->
            approvePayment(paymentKey, orderId, amount)
                .flatMap(approval -> confirmReservationAndSavePayment(reservation, approval)
                    .onErrorResume(dbError -> {
                      log.error("DB 저장 실패, 토스 결제 취소 시도: {}", dbError.getMessage());
                      return paymentService.cancelPayment(paymentKey, "DB 저장 실패로 인한 자동 취소")
                          .onErrorResume(cancelError -> {
                            log.error("토스 결제 취소 실패: {}", cancelError.getMessage());
                            return Mono.empty();
                          })
                          .then(Mono.error(dbError));
                    })
                )
                .onErrorResume(e -> cancelReservationAndReleaseSeat(reservation)
                    .then(Mono.error(e))
                )
        )
        .doFinally(signal -> releaseHold(seatId, userId, token).subscribe())
        .then();
  }

  private Mono<Reservation> reserveSeatAndCreatePending(Long seatId, Long userId) {
    return txOperator.transactional(
        seatRepository.reserveIfAvailable(seatId)
            .filter(updated -> updated == 1)
            .switchIfEmpty(Mono.error(new SeatAlreadyReservedException()))
            .then(seatRepository.findById(seatId))
            .switchIfEmpty(Mono.error(new SeatNotFoundException()))
            .flatMap(seat ->
                reservationRepository.save(
                    Reservation.builder()
                        .userId(userId)
                        .eventId(seat.getEventId())
                        .seatId(seatId)
                        .status(Status.PENDING)
                        .reservedAt(LocalDateTime.now())
                        .build()
                )
            )
    );
  }

  private Mono<Void> confirmReservationAndSavePayment(Reservation reservation,
                                                      PaymentApprovalResponseDto approval) {
    return txOperator.transactional(
        paymentRepository.save(paymentMapper.from(approval))
            .flatMap(payment ->
                reservationRepository.updatePaymentAndStatus(
                    reservation.getId(), payment.getId(), Status.CONFIRMED
                )
            )
            .then()
    );
  }

  private Mono<Void> cancelReservationAndReleaseSeat(Reservation reservation) {
    return txOperator.transactional(
        reservationRepository.updateStatus(reservation.getId(), Status.CANCELLED)
            .then(seatRepository.releaseSeat(reservation.getSeatId()))
            .then()
    );
  }

  private Mono<Void> validateHold(Long seatId, Long userId, String token) {
    return holdService.validateHold(seatId, userId, token)
        .flatMap(valid -> valid ? Mono.empty() : Mono.error(new InvalidHoldException()));
  }

  private Mono<PaymentApprovalResponseDto> approvePayment(String paymentKey, String orderId, Long amount) {
    return paymentService.approvePayment(paymentKey, orderId, amount);
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
