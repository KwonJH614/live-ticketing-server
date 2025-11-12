package org.example.ticketing.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateEmailException extends CustomException{
  public DuplicateEmailException(String email) {
    super("이미 존재하는 email : " + email, HttpStatus.CONFLICT);
  }
}
