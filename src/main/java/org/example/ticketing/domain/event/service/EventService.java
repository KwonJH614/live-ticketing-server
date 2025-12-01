package org.example.ticketing.domain.event.service;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.event.dto.*;
import org.example.ticketing.domain.event.entity.Event;
import org.example.ticketing.domain.event.exception.EventNotFoundException;
import org.example.ticketing.domain.event.exception.InvalidEventTimeException;
import org.example.ticketing.domain.event.exception.PastEventDateException;
import org.example.ticketing.domain.event.repository.EventCustomRepository;
import org.example.ticketing.domain.event.repository.EventRepository;
import org.example.ticketing.domain.seat.service.SeatService;
import org.example.ticketing.domain.user.repository.UserRepository;
import org.example.ticketing.global.exception.UserNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
  private final EventRepository eventRepository;
  private final UserRepository userRepository;
  private final TransactionalOperator txOperator;
  private final EventCustomRepository eventCustomRepository;
  private final SeatService seatService;

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
                    return Mono.error(new UserNotFoundException());
                  }
                  return Mono.just(request);
                })
        )
        .flatMap(request -> {
          if (request.getStartTime().isAfter(request.getEndTime())) {
            return Mono.error(new InvalidEventTimeException());
          }
          return Mono.just(request);
        })
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
        .flatMap(savedEvent ->
            seatService.createSeatsForEvent(savedEvent, dto)
                .then(Mono.just(savedEvent))
        )
        .map(savedEvent -> new CreateEventResponseDto(
            savedEvent.getId(),
            savedEvent.getTitle()
        ))
        .as(txOperator::transactional);
  }

  public Mono<EventDetailDto> getEventDetail(Long id) {
    return eventRepository.findById(id)
        .switchIfEmpty(Mono.error(new EventNotFoundException()))
        .map(event -> EventDetailDto.builder()
            .id(event.getId())
            .title(event.getTitle())
            .description(event.getDescription())
            .startTime(event.getStartTime())
            .endTime(event.getEndTime())
            .venue(event.getVenue())
            .price(event.getPrice())
            .createdAt(event.getCreatedAt())
            .build()
        );
  }

  public Mono<PageResponse<EventListDto>> getEvents(int page, int size) {
    return Mono.zip(
        eventCustomRepository.countAll(),
        eventCustomRepository.findAllPaged(page, size).collectList()
    ).map(tuple -> {
      long totalCount = tuple.getT1();
      List<Event> events = tuple.getT2();

      List<EventListDto> content = events.stream()
          .map(event -> EventListDto.builder()
              .id(event.getId())
              .title(event.getTitle())
              .startTime(event.getStartTime())
              .endTime(event.getEndTime())
              .venue(event.getVenue())
              .price(event.getPrice())
              .createdAt(event.getCreatedAt())
              .build())
          .toList();

      return new PageResponse<>(content, page, size, totalCount);
    });
  }

  public Mono<UpdateEventResponseDto> updateEvent(Long eventId, UpdateEventRequestDto dto) {
    return eventRepository.findById(eventId)
        .switchIfEmpty(Mono.error(new EventNotFoundException()))
        .flatMap(event -> {
          event.setTitle(dto.getTitle());
          event.setDescription(dto.getDescription());
          event.setStartTime(dto.getStartTime());
          event.setEndTime(dto.getEndTime());
          event.setVenue(dto.getVenue());
          event.setPrice(dto.getPrice());
          event.setUpdatedAt(LocalDateTime.now());

          return eventRepository.save(event);
        })
        .map(event -> UpdateEventResponseDto.builder()
            .title(event.getTitle())
            .updatedAt(event.getUpdatedAt())
            .build()
        );
  }

  public Mono<DeleteEventResponseDto> deleteEvent(Long eventId) {
    return eventRepository.findById(eventId)
        .switchIfEmpty(Mono.error(new EventNotFoundException()))
        .flatMap(event -> eventRepository.delete(event)
            .then(Mono.just(new DeleteEventResponseDto(eventId, event.getTitle(), "이벤트가 삭제되었습니다"))));
  }
}
