package org.example.ticketing.domain.event.dto;

import lombok.Builder;

@Builder
public record DeleteEventResponseDto(
  Long id,
  String title,
  String message
) {}
