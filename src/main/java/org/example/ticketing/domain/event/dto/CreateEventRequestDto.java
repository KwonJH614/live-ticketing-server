package org.example.ticketing.domain.event.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CreateEventRequestDto {
  @NotBlank(message = "이벤트명을 입력해주세요")
  private String title;
  @NotBlank(message = "설명을 입력해주세요")
  private String description;
  @NotNull(message = "시작시각 입력해주세요")
  @FutureOrPresent(message = "이벤트 시작시간은 과거일 수 없습니다")
  private LocalDateTime startTime;
  @NotNull(message = "종료시각을 입력해주세요")
  private LocalDateTime endTime;
  @NotBlank(message = "공연장을 입력해주세요")
  private String venue;
  @NotNull(message = "가격을 입력해주세요")
  @Min(value = 0, message = "가격은 0원 이상이어야합니다")
  private int price;
}
