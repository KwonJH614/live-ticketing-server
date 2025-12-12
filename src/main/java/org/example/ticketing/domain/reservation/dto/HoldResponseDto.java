package org.example.ticketing.domain.reservation.dto;

import lombok.Builder;

@Builder
public record HoldResponseDto(
  Long seatId,
  String token
) {}
