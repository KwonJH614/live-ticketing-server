package org.example.ticketing.global.exception;

import org.springframework.http.HttpStatus;

public abstract class CustomException extends RuntimeException {
  private final HttpStatus status;

  public CustomException(String message, HttpStatus status) {
    super(message);
    this.status = status;
  }

  public HttpStatus getStatus() {
    return status;
  }
}
