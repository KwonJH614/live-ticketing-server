package org.example.ticketing.global.exception;

import org.springframework.http.HttpStatus;

public class UserNotFoundException extends CustomException{
  public UserNotFoundException() {
    super("존재하지 않는 유저입니다", HttpStatus.NOT_FOUND);
  }
}