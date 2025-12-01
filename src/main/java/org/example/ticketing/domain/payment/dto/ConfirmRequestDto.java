package org.example.ticketing.domain.payment.dto;

import lombok.Data;

@Data
public class ConfirmRequestDto {
  private String token;
  private String PaymentKey;
  private Long amount;
  private String orderId;
}
