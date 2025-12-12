package org.example.ticketing.domain.event.dto;

import lombok.Builder;

@Builder
public record CreateEventResponseDto(
  Long id,
  String title
) { }
