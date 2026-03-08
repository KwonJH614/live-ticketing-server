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

  /**
   * 대기열 등록 (REST - 최초 1회)
   * 등록 후 WebSocket ws://host/ws/queue/{eventId}?userId={userId} 로 연결하여 실시간 순위 수신
   */
  @PostMapping("{eventId}")
  public Mono<ResponseEntity<ApiResponse<Long>>> registerQueue(
      @PathVariable Long eventId,
      @AuthenticationPrincipal CustomUserPrincipal customUserPrincipal
  ) {
    return eventQueueService.registerQueue(eventId, customUserPrincipal.userId())
        .map(rank -> ResponseEntity.ok(ApiResponse.success(rank)));
  }

  /**
   * 대기열 상태 단건 조회 (REST - 폴링 방식 필요 시 유지, WebSocket 사용 시 선택적)
   * WebSocket 연결이 어려운 환경을 위한 fallback용
   */
  @GetMapping("{eventId}")
  public Mono<ResponseEntity<ApiResponse<QueueStatusDto>>> checkQueueStatus(
      @PathVariable Long eventId,
      @AuthenticationPrincipal CustomUserPrincipal customUserPrincipal
  ) {
    return eventQueueService.getQueueStatus(eventId, customUserPrincipal.userId())
        .map(status -> ResponseEntity.ok(ApiResponse.success(status)));
  }
}