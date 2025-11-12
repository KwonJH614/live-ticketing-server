package org.example.ticketing.domain.user.exception;

import org.example.ticketing.common.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidPasswordException extends CustomException {
  public InvalidPasswordException() {
    super("비밀번호 불일치", HttpStatus.UNAUTHORIZED);
  }
}
