package org.example.ticketing.domain.queue.dto;

import lombok.Builder;
import org.example.ticketing.domain.queue.enums.QueueMessageType;

@Builder
public record QueueWebSocketResponse(
    QueueMessageType type,
    Long rank,          // 현재 대기 순위 (null 가능 - CANCELLED/ERROR 시)
    boolean isAvailable,
    String message
) {
  // 순위 업데이트
  public static QueueWebSocketResponse rankUpdate(Long rank) {
    return QueueWebSocketResponse.builder()
        .type(QueueMessageType.RANK_UPDATE)
        .rank(rank)
        .isAvailable(false)
        .message(rank + "번째 대기 중입니다.")
        .build();
  }

  // 입장 가능
  public static QueueWebSocketResponse available(Long rank) {
    return QueueWebSocketResponse.builder()
        .type(QueueMessageType.AVAILABLE)
        .rank(rank)
        .isAvailable(true)
        .message("입장 가능합니다! 지금 이벤트 페이지로 이동하세요.")
        .build();
  }

  // 대기열 취소
  public static QueueWebSocketResponse cancelled() {
    return QueueWebSocketResponse.builder()
        .type(QueueMessageType.CANCELLED)
        .isAvailable(false)
        .message("대기열에서 취소되었습니다.")
        .build();
  }

  // 에러
  public static QueueWebSocketResponse error(String errorMessage) {
    return QueueWebSocketResponse.builder()
        .type(QueueMessageType.ERROR)
        .isAvailable(false)
        .message(errorMessage)
        .build();
  }
}