package org.example.ticketing.domain.reservation.exception;

import org.example.ticketing.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class ReservationNotFoundException extends CustomException {
  public ReservationNotFoundException() {
    super("예약을 찾을 수 없습니다", HttpStatus.NOT_FOUND);
  }
}
