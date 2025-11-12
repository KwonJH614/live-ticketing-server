package org.example.ticketing.domain.event.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EventListDto {
  private String title;
  private String venue;
  private LocalDateTime createdAt;
}
