package org.example.ticketing.domain.Queue.dto;

import lombok.Builder;

@Builder
public record QueueStatusDto(
  Long rank,
  boolean isAvailable
) {}
