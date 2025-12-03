package org.example.ticketing.domain.event.exception;

import lombok.Getter;
import org.example.ticketing.global.exception.CustomException;
import org.springframework.http.HttpStatus;

@Getter
public class QueueWaitingException extends CustomException {
  private final Long rank;

  public QueueWaitingException(Long rank) {
    super("대기 중입니다. 현재 순번 " + rank, HttpStatus.FORBIDDEN);
    this.rank = rank;
  }
}
