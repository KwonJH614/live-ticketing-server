package org.example.ticketing.domain.payment.exception;

import org.example.ticketing.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class PaymentNotFoundException extends CustomException {
  public PaymentNotFoundException() {
    super("결제 정보를 찾을 수 없습니다", HttpStatus.NOT_FOUND);
  }
}
