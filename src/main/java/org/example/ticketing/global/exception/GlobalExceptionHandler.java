package org.example.ticketing.global.exception;

import org.example.ticketing.global.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.MissingRequestValueException;
import reactor.core.publisher.Mono;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  // Validation 에러 처리
  @ExceptionHandler(WebExchangeBindException.class)
  public Mono<ResponseEntity<ApiResponse<Void>>> handleValidationException(WebExchangeBindException e) {
    String errorMessage = e.getBindingResult().getFieldErrors().stream()
        .map(FieldError::getDefaultMessage)
        .collect(Collectors.joining(", "));
    log.warn("Validation Exception: {}", errorMessage);
    return Mono.just(ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.fail(errorMessage)));
  }

  // CustomException 처리
  @ExceptionHandler(CustomException.class)
  public Mono<ResponseEntity<ApiResponse<Void>>> handleCustomException(CustomException e) {
    log.warn("Business Exception: {}", e.getMessage());
    return Mono.just(ResponseEntity
        .status(e.getStatus())
        .body(ApiResponse.fail(e.getMessage())));
  }

  // BAD_REQUEST 처리
  @ExceptionHandler(MissingRequestValueException.class)
  public Mono<ResponseEntity<ApiResponse<Void>>> handleMissingRequestValueException(MissingRequestValueException e) {
    log.warn("Missing Request Parameter: {}", e.getReason());
    return Mono.just(ResponseEntity
        .status(HttpStatus.BAD_REQUEST)
        .body(ApiResponse.fail("잘못된 요청입니다: " + e.getReason())));
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
