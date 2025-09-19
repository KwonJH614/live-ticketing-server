package org.example.ticketing.domain.user.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.ticketing.domain.user.enums.Role;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@Table("users")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
  @Id
  private Long id;
  private String email;
  private String username;
  private String password;
  private Role role;

  @CreatedDate
  private LocalDateTime createdAt;
}
