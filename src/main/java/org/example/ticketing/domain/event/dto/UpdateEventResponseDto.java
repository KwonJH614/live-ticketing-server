package org.example.ticketing.domain.event.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record UpdateEventResponseDto(
  Long id,
  String title,
  LocalDateTime updatedAt
) {}
