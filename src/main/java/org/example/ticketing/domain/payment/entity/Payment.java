package org.example.ticketing.domain.payment.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ticketing.domain.payment.enums.PaymentStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("payments")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {
  @Id
  private Long id;
  @Column("order_id")
  private String orderId;
  @Column("payment_key")
  private String paymentKey;
  private Long amount;
  private PaymentStatus status;
  @Column("created_at")
  private LocalDateTime createdAt;
}
