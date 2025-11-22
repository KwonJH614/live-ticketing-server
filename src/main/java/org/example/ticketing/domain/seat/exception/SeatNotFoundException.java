package org.example.ticketing.domain.seat.exception;

import org.example.ticketing.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class SeatNotFoundException extends CustomException {
  public SeatNotFoundException() {
    super("좌석을 찾을 수 없습니다", HttpStatus.NOT_FOUND);
  }
}
