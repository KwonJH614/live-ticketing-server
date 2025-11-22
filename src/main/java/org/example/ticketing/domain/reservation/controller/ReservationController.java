package org.example.ticketing.domain.reservation.controller;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.reservation.dto.ConfirmRequestDto;
import org.example.ticketing.domain.reservation.dto.HoldResponseDto;
import org.example.ticketing.domain.reservation.service.ReservationHoldService;
import org.example.ticketing.domain.reservation.service.ReservationService;
import org.example.ticketing.global.response.ApiResponse;
import org.example.ticketing.security.CustomUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/reservation")
@RequiredArgsConstructor
public class ReservationController {
  private final ReservationService reservationService;
  private final ReservationHoldService holdService;

  @PostMapping("/{seatId}")
  public Mono<ResponseEntity<ApiResponse<HoldResponseDto>>> holdSeat(
          @PathVariable Long seatId,
          @AuthenticationPrincipal CustomUserPrincipal principal
  )
  {
    return holdService.holdSeat(seatId, principal.userId())
            .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
  }

  @PostMapping("/confirm/{seatId}")
  public Mono<ResponseEntity<ApiResponse<Void>>> confirm(
          @PathVariable Long seatId,
          @RequestBody ConfirmRequestDto confirmRequestDto,
          @AuthenticationPrincipal CustomUserPrincipal principal
  )
  {
    return reservationService.confirmReservation(
            seatId, principal.userId(), confirmRequestDto.getToken())
            .then(Mono.fromCallable(() -> ResponseEntity.ok(ApiResponse.success())));
  }
}
