package org.example.ticketing.domain.Queue.enums;

public enum QueueMessageType {
  RANK_UPDATE,   // 순위 업데이트 (3초마다)
  AVAILABLE,     // 입장 가능
  CANCELLED,     // 대기열 취소 완료
  ERROR          // 오류
}