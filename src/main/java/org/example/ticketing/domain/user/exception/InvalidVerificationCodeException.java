package org.example.ticketing.domain.user.exception;

import org.example.ticketing.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidVerificationCodeException extends CustomException {
  public InvalidVerificationCodeException() {
    super("잘못된 인증코드입니다", HttpStatus.BAD_REQUEST);
  }
}
