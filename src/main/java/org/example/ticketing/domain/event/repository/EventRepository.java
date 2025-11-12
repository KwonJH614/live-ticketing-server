package org.example.ticketing.domain.event.repository;

import org.example.ticketing.domain.event.entity.Event;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface EventRepository extends ReactiveCrudRepository<Event, Long> {
}
