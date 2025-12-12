package org.example.ticketing.domain.user.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequestDto(
  @NotBlank(message = "username을 입력해주세요")
  String username,

  @NotBlank(message = "password를 입력해주세요")
  String password
) {}
