package org.example.ticketing.domain.event.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.event.exception.PastEventDateException;
import org.example.ticketing.common.exception.UserNotFoundException;
import org.example.ticketing.domain.event.dto.CreateEventRequestDto;
import org.example.ticketing.domain.event.dto.CreateEventResponseDto;
import org.example.ticketing.domain.event.entity.Event;
import org.example.ticketing.domain.event.repository.EventRepository;
import org.example.ticketing.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EventService {
  private final EventRepository eventRepository;
  private final UserRepository userRepository;

  public Mono<CreateEventResponseDto> createEvent(CreateEventRequestDto dto, String username) {
    return Mono.just(dto)
            .flatMap(request -> {
              if (request.getStartTime().isBefore(LocalDateTime.now())) {
                return Mono.error(new PastEventDateException());
              }
              return Mono.just(request);
            })
            .flatMap(request ->
              userRepository.existsByUsername(username)
                      .flatMap(exists -> {
                        if (!exists) {
                          return Mono.error(new UserNotFoundException(username));
                        }
                        return Mono.just(request);
                      })
            )
            .flatMap(request -> {
              Event event = Event.builder()
                      .title(request.getTitle())
                      .description(request.getDescription())
                      .startTime(request.getStartTime())
                      .endTime(request.getEndTime())
                      .venue(request.getVenue())
                      .price(request.getPrice())
                      .build();

              return eventRepository.save(event);
            })
            .map(savedEvent -> new CreateEventResponseDto(
                    savedEvent.getId(),
                    savedEvent.getTitle()
            ));
  }
}
