package org.example.ticketing.domain.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.event.dto.*;
import org.example.ticketing.domain.Queue.exception.QueueWaitingException;
import org.example.ticketing.domain.Queue.service.EventQueueService;
import org.example.ticketing.domain.event.service.EventService;
import org.example.ticketing.global.dto.PageResponse;
import org.example.ticketing.global.response.ApiResponse;
import org.example.ticketing.security.CustomUserPrincipal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/events")
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
    Long userId = customUserPrincipal.userId();

    return eventQueueService.hasAccess(id, userId)
        .flatMap(hasAccess -> {
          if (hasAccess) {
            return eventService.getEventDetail(id)
                .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
          }

          return eventQueueService.getQueueStatus(id, userId)
              .flatMap(status -> {
                if (!status.isAvailable()) {
                  return Mono.error(new QueueWaitingException(status.rank()));
                }

                return eventQueueService.grantAccess(id, userId)
                    .flatMap(granted -> {
                      if (!Boolean.TRUE.equals(granted)) {
                        return Mono.error(new IllegalStateException("Failed to grant access token"));
                      }
                      return eventQueueService.deleteQueue(id, userId)
                          .flatMap(deleted -> {
                            if (!Boolean.TRUE.equals(deleted)) {
                              return eventQueueService.revokeAccess(id, userId)
                                  .then(Mono.error(new IllegalStateException("Failed to remove queue entry")));
                            }
                            return eventService.getEventDetail(id)
                                .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
                          })
                          .onErrorResume(ex -> eventQueueService.revokeAccess(id, userId)
                              .then(Mono.error(ex)));
                    });
              });
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
