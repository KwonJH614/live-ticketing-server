package org.example.ticketing.domain.event.exception;

import org.example.ticketing.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class EventNotFoundException extends CustomException {
  public EventNotFoundException() {
    super("이벤트를 찾을 수 없습니다", HttpStatus.NOT_FOUND);
  }
}
