package org.example.ticketing.domain.reservation.exception;

import org.example.ticketing.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidHoldAccessException extends CustomException {
  public InvalidHoldAccessException() {
    super("홀드를 해제할 권한이 없습니다", HttpStatus.FORBIDDEN);
  }
}
