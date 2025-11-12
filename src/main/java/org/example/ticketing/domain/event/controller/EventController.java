package org.example.ticketing.domain.event.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.ticketing.common.response.ApiResponse;
import org.example.ticketing.domain.event.dto.CreateEventRequestDto;
import org.example.ticketing.domain.event.dto.CreateEventResponseDto;
import org.example.ticketing.domain.event.repository.EventRepository;
import org.example.ticketing.domain.event.service.EventService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
}
