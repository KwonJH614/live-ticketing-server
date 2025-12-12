package org.example.ticketing.domain.reservation.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReservationListDto(
  Long id,
  String eventName,
  String seatNumber,
  String status,
  LocalDateTime reservedAt
) {}
