package org.example.ticketing.common.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends CustomException{
  public UserNotFoundException(String username) {
    super("존재하지 않는 username : " + username, HttpStatus.NOT_FOUND);
  }
}