package org.example.ticketing.common.exception;

import org.springframework.http.HttpStatus;

public class PastEventDateException extends CustomException{
  public PastEventDateException() {
    super("시작날짜는 오늘 이후여야 합니다", HttpStatus.BAD_REQUEST);
  }
}
