package org.example.ticketing.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
public record RegisterRequest(
  @NotBlank(message = "username은 필수입니다")
  String username,

  @NotBlank(message = "email은 필수입니다")
  String email,

  @NotBlank(message = "password는 필수입니다")
  @Size(min = 6, message = "password는 최소 6자리 이상이어야 합니다")
  String password,

  @NotBlank(message = "인증코드는 필수입니다")
  @Pattern(regexp = "\\d{6}", message = "인증코드는 6자리 숫자여야합니다")
  String verificationCode
) {}
