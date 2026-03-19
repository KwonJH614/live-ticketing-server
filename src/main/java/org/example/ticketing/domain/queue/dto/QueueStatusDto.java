package org.example.ticketing.domain.queue.dto;

import lombok.Builder;

@Builder
public record QueueStatusDto(
  Long rank,
  boolean isAvailable
) {}
