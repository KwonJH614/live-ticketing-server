package org.example.ticketing.domain.event.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record CreateEventRequestDto(
    @NotBlank(message = "이벤트명을 입력해주세요")
    String title,

    @NotBlank(message = "설명을 입력해주세요")
    String description,

    @NotNull(message = "시작시각 입력해주세요")
    @FutureOrPresent(message = "이벤트 시작시간은 과거일 수 없습니다")
    LocalDateTime startTime,

    @NotNull(message = "종료시각을 입력해주세요")
    LocalDateTime endTime,

    @NotBlank(message = "공연장을 입력해주세요")
    String venue,

    @NotNull(message = "가격을 입력해주세요")
    @Min(value = 0, message = "가격은 0원 이상이어야합니다")
    Integer price,

    @NotNull(message = "행을 입력해주세요")
    @Min(value = 1, message = "행은 1 이상이어야합니다")
    Integer row,

    @NotNull(message = "좌석 수를 입력해주세요")
    @Min(value = 1, message = "좌석은 1석 이상이어야합니다")
    Integer column
) {}
