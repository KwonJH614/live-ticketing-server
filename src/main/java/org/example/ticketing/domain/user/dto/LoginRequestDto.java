package org.example.ticketing.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequestDto {

  @NotBlank(message = "email을 입력해주세요")
  private String email;

  @NotBlank(message = "password를 입력해주세요")
  private String password;
}
