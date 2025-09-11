package org.example.ticketing.common.exception;

import org.example.ticketing.common.exception.CustomException;
import org.example.ticketing.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // CustomException 처리
  @ExceptionHandler(CustomException.class)
  public Mono<ResponseEntity<ApiResponse<Void>>> handleCustomException(CustomException e) {
    log.warn("Business Exception: {}", e.getMessage());
    return Mono.just(ResponseEntity
            .status(e.getStatus())
            .body(ApiResponse.fail(e.getMessage())));
  }

  // 그 외 모든 예외 처리
  @ExceptionHandler(Exception.class)
  public Mono<ResponseEntity<ApiResponse<Void>>> handleException(Exception e) {
    log.error("서버 에러 발생", e);
    return Mono.just(ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.fail("서버 에러 발생")));
  }
}
