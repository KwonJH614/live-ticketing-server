package org.example.ticketing.domain.reservation.exception;

import org.example.ticketing.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class QueueAccessRequiredException extends CustomException {
  public QueueAccessRequiredException() {
    super("대기열을 통과해야 좌석을 선점할 수 있습니다", HttpStatus.FORBIDDEN);
  }
}
