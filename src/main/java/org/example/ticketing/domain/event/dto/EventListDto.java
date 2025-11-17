package org.example.ticketing.domain.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventListDto {
  private Long id;
  private String title;
  private LocalDateTime startTime;
  private LocalDateTime endTime;
  private String venue;
  private int price;
  private LocalDateTime createdAt;
}
