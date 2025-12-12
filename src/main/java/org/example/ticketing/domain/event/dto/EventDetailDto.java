package org.example.ticketing.domain.event.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EventDetailDto(
  Long id,
  String title,
  String description,
  LocalDateTime startTime,
  LocalDateTime endTime,
  String venue,
  int price,
  LocalDateTime createdAt
) {}
