package org.example.ticketing.domain.payment.exception;

import org.example.ticketing.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class AmountMisMatchException extends CustomException {
  public AmountMisMatchException() {
    super("가격이 일치하지 않습니다", HttpStatus.BAD_REQUEST);
  }
}
