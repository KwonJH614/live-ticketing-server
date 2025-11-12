package org.example.ticketing.domain.event.exception;

import org.example.ticketing.global.exception.CustomException;
import org.springframework.http.HttpStatus;

public class InvalidEventTimeException extends CustomException {
    public InvalidEventTimeException() {
        super("이벤트 시작 시간은 종료 시간보다 이전이어야 합니다", HttpStatus.BAD_REQUEST);
    }
}
