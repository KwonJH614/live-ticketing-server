package org.example.ticketing.domain.payment.dto;

import lombok.Builder;

@Builder
public record PaymentApprovalResponseDto(
  String paymentKey,
  String orderId,
  Long amount
) {}
