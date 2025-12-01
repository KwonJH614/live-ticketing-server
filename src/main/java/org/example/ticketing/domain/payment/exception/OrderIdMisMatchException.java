package org.example.ticketing.domain.payment.exception;

import org.example.ticketing.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class OrderIdMisMatchException extends CustomException{
  public OrderIdMisMatchException() {
    super("OrderId가 일치하지 않습니다.", HttpStatus.BAD_REQUEST);
  }
}
