package org.example.ticketing.common.exception;

import org.springframework.http.HttpStatus;

public class DuplicateUsernameException extends CustomException{
  public DuplicateUsernameException(String username) {
    super("이미 존재하는 username : " + username, HttpStatus.CONFLICT);
  }
}
