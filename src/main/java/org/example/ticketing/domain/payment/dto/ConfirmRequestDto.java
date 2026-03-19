package org.example.ticketing.domain.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ConfirmRequestDto(
  @NotBlank(message = "token은 필수입니다")
  String token,

  @NotBlank(message = "paymentKey는 필수입니다")
  String paymentKey,

  @NotNull(message = "amount는 필수입니다")
  @Positive(message = "amount는 양수여야 합니다")
  Long amount,

  @NotBlank(message = "orderId는 필수입니다")
  String orderId
) {
}
