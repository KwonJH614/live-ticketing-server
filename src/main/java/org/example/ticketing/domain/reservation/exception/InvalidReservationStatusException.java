package org.example.ticketing.domain.reservation.exception;

import org.example.ticketing.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidReservationStatusException extends CustomException {
  public InvalidReservationStatusException() {
    super("취소할 수 없는 예약 상태입니다", HttpStatus.CONFLICT);
  }
}
