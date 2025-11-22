package org.example.ticketing.domain.reservation.exception;

import org.example.ticketing.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class SeatAlreadyHeldException extends CustomException {
  public SeatAlreadyHeldException() {
    super("이미 다른 사용자가 예매중인 좌석입니다", HttpStatus.CONFLICT);
  }
}
