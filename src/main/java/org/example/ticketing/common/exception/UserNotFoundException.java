package org.example.ticketing.common.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends CustomException{
  public UserNotFoundException(String email) {
    super("가입되지 않은 email : " + email, HttpStatus.NOT_FOUND);
  }
}