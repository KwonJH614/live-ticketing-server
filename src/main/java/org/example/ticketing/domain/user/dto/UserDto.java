package org.example.ticketing.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDto {
  @NotBlank(message = "username은 필수입니다")
  private String username;

  @NotBlank(message = "email은 필수입니다")
  private String email;

  @NotBlank(message = "password는 필수입니다")
  @Size(min = 6, message = "password는 최소 6자리 이상이어야 합니다")
  private String password;
}
