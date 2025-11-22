package org.example.ticketing.domain.reservation.exception;

import org.example.ticketing.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class SeatAlreadyReservedException extends CustomException {
  public SeatAlreadyReservedException() {
    super("이미 다른 사용자가 예매한 좌석입니다", HttpStatus.CONFLICT);
  }
}
