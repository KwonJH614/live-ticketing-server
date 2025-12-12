package org.example.ticketing.domain.event.dto;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EventListDto(
  Long id,
  String title,
  LocalDateTime startTime,
  LocalDateTime endTime,
  String venue,
  int price,
  LocalDateTime createdAt
) {}
