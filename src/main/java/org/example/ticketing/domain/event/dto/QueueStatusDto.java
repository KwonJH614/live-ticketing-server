package org.example.ticketing.domain.event.dto;

import lombok.Builder;

@Builder
public record QueueStatusDto(
  Long rank,
  boolean isAvailable
) {}
