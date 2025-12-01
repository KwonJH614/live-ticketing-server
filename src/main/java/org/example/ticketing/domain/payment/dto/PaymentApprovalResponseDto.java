package org.example.ticketing.domain.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentApprovalResponseDto {
  private String paymentKey;
  private String orderId;
  private Long amount;
}
