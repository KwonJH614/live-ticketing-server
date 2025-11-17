package org.example.ticketing.domain.seat.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table("seats")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Seat {

  @Id
  private Long id;
  @Column("event_id")
  private Long eventId;
  @Column("seat_number")
  private String seatNumber;
  @Column("is_reserved")
  private boolean isReserved;
}
