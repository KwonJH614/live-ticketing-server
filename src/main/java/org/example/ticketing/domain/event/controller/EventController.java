package org.example.ticketing.domain.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.event.dto.*;
import org.example.ticketing.domain.event.exception.QueueWaitingException;
import org.example.ticketing.domain.event.service.EventQueueService;
import org.example.ticketing.domain.event.service.EventService;
import org.example.ticketing.global.dto.PageResponse;
import org.example.ticketing.global.response.ApiResponse;
import org.example.ticketing.security.CustomUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/event")
@RequiredArgsConstructor
public class EventController {
  private final EventService eventService;
  private final EventQueueService eventQueueService;

  @PostMapping
  public Mono<ResponseEntity<ApiResponse<CreateEventResponseDto>>> createEvent(
      @Valid @RequestBody CreateEventRequestDto createEventRequestDto,
      @AuthenticationPrincipal CustomUserPrincipal customUserPrincipal
  ) {
    return eventService.createEvent(createEventRequestDto, customUserPrincipal.username())
        .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<ApiResponse<EventDetailDto>>> getEventDetail(
      @PathVariable Long id,
      @AuthenticationPrincipal CustomUserPrincipal customUserPrincipal
  ) {
    return eventQueueService.getQueueStatus(id, customUserPrincipal.username())
        .flatMap(status -> {
          if (!status.isAvailable()) {
            return Mono.error(new QueueWaitingException(status.getRank()));
          }

          return eventService.getEventDetail(id)
              .flatMap(response ->
                  eventQueueService.deleteQueue(response.getId(), customUserPrincipal.username())
                      .thenReturn(ResponseEntity.ok(ApiResponse.success(response))));
        });
  }

  @GetMapping
  public Mono<ResponseEntity<ApiResponse<PageResponse<EventListDto>>>> getAllEvent(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size
  ) {
    return eventService.getEvents(page, size)
        .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
  }

  @PutMapping("/{id}")
  public Mono<ResponseEntity<ApiResponse<UpdateEventResponseDto>>> updateEvent(
      @PathVariable Long id,
      @Valid @RequestBody UpdateEventRequestDto updateEventRequestDto
  ) {
    return eventService.updateEvent(id, updateEventRequestDto)
        .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<ApiResponse<DeleteEventResponseDto>>> deleteEvent(
      @PathVariable Long id
  ) {
    return eventService.deleteEvent(id)
        .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
  }
}
