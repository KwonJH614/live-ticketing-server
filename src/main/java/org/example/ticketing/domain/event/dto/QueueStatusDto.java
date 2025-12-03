package org.example.ticketing.domain.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class QueueStatusDto {
  private Long rank;
  private boolean isAvailable;
}
