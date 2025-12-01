package org.example.ticketing.domain.reservation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReservationListDto {
  private Long id;
  private String eventName;
  private String seatNumber;
  private String status;
  private LocalDateTime reservedAt;
}
