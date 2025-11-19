package org.example.ticketing.domain.seat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatListDto {
  private Long id;
  private String seatNumber;
  private boolean isReserved;
}
