package org.example.ticketing.domain.user.exception;

import org.example.ticketing.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class EmailSendException extends CustomException {
  public EmailSendException(String message) {
    super(message, HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
