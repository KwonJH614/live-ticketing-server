package org.example.ticketing.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateUserException extends CustomException{
  public DuplicateUserException(String email) {
    super("이미 존재하는 email : " + email, HttpStatus.CONFLICT);
  }
}
