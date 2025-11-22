package org.example.ticketing.domain.reservation.exception;

import org.example.ticketing.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidHoldException extends CustomException {
  public InvalidHoldException() {
    super("유효하지 않은 홀드 토큰입니다", HttpStatus.BAD_REQUEST);
  }
}
