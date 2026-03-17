package org.example.ticketing.domain.payment.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticketing.domain.payment.dto.PaymentApprovalResponseDto;
import org.example.ticketing.domain.payment.exception.AmountMisMatchException;
import org.example.ticketing.domain.payment.exception.OrderIdMisMatchException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TossPaymentService {

  @Value("${toss.secret.key}")
  private String secretKey;

  private final WebClient webClient;

  public Mono<PaymentApprovalResponseDto> approvePayment(
      String paymentKey,
      String orderId,
      Long amount
  ) {
    String basicAuth = "Basic " + Base64.getEncoder()
        .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

    Map<String, Object> body = Map.of(
        "paymentKey", paymentKey,
        "orderId", orderId,
        "amount", amount
    );

    return webClient.post()
        .uri("https://api.tosspayments.com/v1/payments/confirm")
        .header(HttpHeaders.AUTHORIZATION, basicAuth)
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
        .bodyValue(body)
        .retrieve()
        .onStatus(HttpStatusCode::is4xxClientError, response ->
            response.bodyToMono(String.class)
                .flatMap(bodyStr -> {
                  log.error("토스 4xx 에러: {}", bodyStr);
                  return Mono.error(new RuntimeException("토스 요청 오류(4xx): " + bodyStr));
                })
        )
        .onStatus(HttpStatusCode::is5xxServerError, response ->
            response.bodyToMono(String.class)
                .flatMap(bodyStr -> {
                  log.error("토스 5xx 에러: {}", bodyStr);
                  return Mono.error(new RuntimeException("토스 서버 오류(5xx): " + bodyStr));
                })
        )
        .bodyToMono(Map.class)
        .flatMap(response -> {
          if (!orderId.equals(response.get("orderId"))) {
            return Mono.error(new OrderIdMisMatchException());
          }
          if (!amount.equals(((Number) response.get("totalAmount")).longValue())) {
            return Mono.error(new AmountMisMatchException());
          }

          PaymentApprovalResponseDto dto = PaymentApprovalResponseDto.builder()
              .paymentKey(paymentKey)
              .orderId(orderId)
              .amount(amount)
              .build();

          log.info("토스 결제 승인 응답: {}", dto);
          return Mono.just(dto);
        })
        .switchIfEmpty(Mono.error(new RuntimeException("토스 응답이 비어 있습니다 (null body)")));
  }
}
