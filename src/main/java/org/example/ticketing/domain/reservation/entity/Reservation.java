package org.example.ticketing.domain.reservation.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ticketing.domain.reservation.enums.Status;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("reservations")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Reservation {
  @Id
  Long id;
  @Column("user_id")
  Long userId;
  @Column("event_id")
  Long eventId;
  @Column("seat_id")
  Long seatId;
  @Column("payment_id")
  Long paymentId;
  private Status status;
  @Column("reserved_at")
  private LocalDateTime reservedAt;
}
