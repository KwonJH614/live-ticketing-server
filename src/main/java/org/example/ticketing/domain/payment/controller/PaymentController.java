package org.example.ticketing.domain.payment.controller;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.payment.dto.ConfirmRequestDto;
import org.example.ticketing.domain.reservation.service.ReservationHoldService;
import org.example.ticketing.domain.reservation.service.ReservationService;
import org.example.ticketing.global.response.ApiResponse;
import org.example.ticketing.security.CustomUserPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {
  private final ReservationHoldService holdService;
  private final ReservationService reservationService;

  @Value("${toss.client.key}")
  private String clientKey;

  @GetMapping("/client-key")
  public Mono<ResponseEntity<ApiResponse<String>>> getClientKey() {
    return Mono.just(ResponseEntity.ok(ApiResponse.success(clientKey)));
  }

  @PostMapping("/confirm/{seatId}")
  public Mono<ResponseEntity<ApiResponse<Void>>> confirm(
      @PathVariable Long seatId,
      @RequestBody ConfirmRequestDto confirmRequestDto,
      @AuthenticationPrincipal CustomUserPrincipal principal
  ) {
    return reservationService.confirmReservation(
            seatId,
            principal.userId(),
            confirmRequestDto.getToken(),
            confirmRequestDto.getPaymentKey(),
            confirmRequestDto.getAmount(),
            confirmRequestDto.getOrderId()
        )
        .then(Mono.fromCallable(() -> ResponseEntity.ok(ApiResponse.success())));
  }

}
