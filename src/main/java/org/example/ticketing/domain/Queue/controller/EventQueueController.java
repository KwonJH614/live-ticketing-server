package org.example.ticketing.domain.Queue.controller;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.Queue.dto.QueueStatusDto;
import org.example.ticketing.domain.Queue.service.EventQueueService;
import org.example.ticketing.global.response.ApiResponse;
import org.example.ticketing.security.CustomUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/queue")
@RequiredArgsConstructor
public class EventQueueController {

  private final EventQueueService eventQueueService;

  @PostMapping("{eventId}")
  public Mono<ResponseEntity<ApiResponse<Long>>> registerQueue(
      @PathVariable Long eventId,
      @AuthenticationPrincipal CustomUserPrincipal customUserPrincipal
  ) {
    return eventQueueService.registerQueue(eventId, customUserPrincipal.userId())
        .map(rank -> ResponseEntity.ok(ApiResponse.success(rank)));
  }

  @GetMapping("{eventId}")
  public Mono<ResponseEntity<ApiResponse<QueueStatusDto>>> checkQueueStatus(
      @PathVariable Long eventId,
      @AuthenticationPrincipal CustomUserPrincipal customUserPrincipal
  ) {
    return eventQueueService.getQueueStatus(eventId, customUserPrincipal.userId())
        .map(status -> ResponseEntity.ok(ApiResponse.success(status)));
  }
}