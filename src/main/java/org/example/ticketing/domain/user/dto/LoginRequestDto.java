package org.example.ticketing.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDto {
  @NotBlank(message = "username을 입력해주세요")
  private String username;

  @NotBlank(message = "password를 입력해주세요")
  private String password;
}
