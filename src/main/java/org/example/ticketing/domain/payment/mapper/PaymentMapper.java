package org.example.ticketing.domain.payment.mapper;

import org.example.ticketing.domain.payment.dto.PaymentApprovalResponseDto;
import org.example.ticketing.domain.payment.entity.Payment;
import org.example.ticketing.domain.payment.enums.PaymentStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class PaymentMapper {

  public Payment from(PaymentApprovalResponseDto approval) {
    return Payment.builder()
        .orderId(approval.orderId())
        .paymentKey(approval.paymentKey())
        .amount(approval.amount())
        .status(PaymentStatus.SUCCESS)
        .createdAt(LocalDateTime.now())
        .build();
  }
}
