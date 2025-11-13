package org.example.ticketing.domain.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.event.dto.*;
import org.example.ticketing.global.response.ApiResponse;
import org.example.ticketing.domain.event.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/api/event")
@RequiredArgsConstructor
public class EventController {
  private final EventService eventService;

  @PostMapping
  public Mono<ResponseEntity<ApiResponse<CreateEventResponseDto>>> createEvent(
          @Valid @RequestBody CreateEventRequestDto createEventRequestDto,
          Authentication authentication
  ) {
    return eventService.createEvent(createEventRequestDto, authentication.getName())
            .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
  }

  @GetMapping("/{id}")
  public Mono<ResponseEntity<ApiResponse<EventDetailDto>>> getEventDetail(
          @PathVariable Long id
  ) {
    return eventService.getEventDetail(id)
            .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
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
            .map(response ->  ResponseEntity.ok(ApiResponse.success(response)));
  }

  @DeleteMapping("/{id}")
  public Mono<ResponseEntity<ApiResponse<DeleteEventResponseDto>>> deleteEvent(
          @PathVariable Long id
  ) {
    return eventService.deleteEvent(id)
            .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
  }
}
