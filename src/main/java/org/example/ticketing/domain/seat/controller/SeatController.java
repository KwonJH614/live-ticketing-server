package org.example.ticketing.domain.seat.controller;

import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.seat.dto.SeatListDto;
import org.example.ticketing.domain.seat.service.SeatService;
import org.example.ticketing.global.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/api/seat")
@RequiredArgsConstructor
public class SeatController {
  private final SeatService seatService;

  @GetMapping("/{id}")
  public Mono<ResponseEntity<ApiResponse<List<SeatListDto>>>> getSeats(
          @PathVariable Long id
  )
  {
    return seatService.getSeats(id)
            .map(response -> ResponseEntity.ok(ApiResponse.success(response)));
  }
}
