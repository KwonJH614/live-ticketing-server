package org.example.ticketing.domain.event.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeleteEventResponseDto {
  private Long id;
  private String title;
  private String message;
}
