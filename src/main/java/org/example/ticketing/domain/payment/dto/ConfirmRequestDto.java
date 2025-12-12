package org.example.ticketing.domain.payment.dto;

public record ConfirmRequestDto(
  String token,
  String paymentKey,
  Long amount,
  String orderId
) {
}
