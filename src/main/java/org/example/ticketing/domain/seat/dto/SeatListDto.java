package org.example.ticketing.domain.seat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
public record SeatListDto(
  Long id,
  String seatNumber,
  boolean isReserved
) {}
