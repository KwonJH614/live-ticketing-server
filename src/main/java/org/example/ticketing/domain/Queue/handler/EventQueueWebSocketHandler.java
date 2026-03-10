package org.example.ticketing.domain.Queue.handler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.ticketing.domain.Queue.dto.QueueStatusDto;
import org.example.ticketing.domain.Queue.dto.QueueWebSocketResponse;
import org.example.ticketing.domain.Queue.enums.QueueMessageType;
import org.example.ticketing.domain.Queue.service.EventQueueService;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventQueueWebSocketHandler implements WebSocketHandler {

  private final EventQueueService eventQueueService;
  private final ObjectMapper objectMapper;

  // 3초마다 push
  private static final Duration POLL_INTERVAL = Duration.ofSeconds(3);

  @Override
  public Mono<Void> handle(WebSocketSession session) {
    Long eventId = extractEventId(session);
    Long userId = extractUserId(session);

    // 파라미터 검증 실패 시 즉시 종료
    if (eventId == null || userId == null) {
      log.warn("WebSocket 연결 실패 - 파라미터 누락: uri={}", session.getHandshakeInfo().getUri());
      return sendAndClose(session,
          QueueWebSocketResponse.error("eventId 또는 userId가 올바르지 않습니다."),
          CloseStatus.BAD_DATA);
    }

    log.info("WebSocket 연결: eventId={}, userId={}", eventId, userId);

    // 클라이언트 → 서버 수신 처리 (CANCEL)
    Mono<Void> receive = session.receive()
        .flatMap(msg -> handleInboundMessage(session, msg.getPayloadAsText(), eventId, userId))
        .doOnError(e -> log.error("수신 오류: eventId={}, userId={}", eventId, userId, e))
        .then();

    // 서버 → 클라이언트 주기 push
    Flux<WebSocketMessage> send = buildQueueStatusFlux(session, eventId, userId);

    return session.send(send)
        .and(receive)
        .doFinally(signal -> log.info("WebSocket 종료: eventId={}, userId={}, signal={}", eventId, userId, signal));
  }

  // ──────────────────────────────────────────────
  // 서버 → 클라이언트 스트림
  // ──────────────────────────────────────────────

  private Flux<WebSocketMessage> buildQueueStatusFlux(WebSocketSession session, Long eventId, Long userId) {
    return Flux.interval(Duration.ZERO, POLL_INTERVAL)
        .flatMap(tick -> eventQueueService.getQueueStatus(eventId, userId))
        .map(status -> toWebSocketResponse(status))
        .distinctUntilChanged(QueueWebSocketResponse::rank)
        .map(response -> toWebSocketMessage(session, response))
        // AVAILABLE 메시지 전송 후 스트림 자동 종료
        .takeUntil(msg -> isType(msg, QueueMessageType.AVAILABLE))
        .doOnError(e -> log.error("push 오류: eventId={}, userId={}", eventId, userId, e))
        .onErrorResume(e -> Flux.just(
            toWebSocketMessage(session, QueueWebSocketResponse.error("서버 오류가 발생했습니다."))
        ));
  }

  private QueueWebSocketResponse toWebSocketResponse(QueueStatusDto status) {
    if (status.isAvailable()) {
      return QueueWebSocketResponse.available(status.rank());
    }
    return QueueWebSocketResponse.rankUpdate(status.rank());
  }

  // ──────────────────────────────────────────────
  // 클라이언트 → 서버 메시지 처리
  // ──────────────────────────────────────────────

  private Mono<Void> handleInboundMessage(WebSocketSession session, String payload,
                                          Long eventId, Long userId) {
    String command = payload.trim().toUpperCase();
    log.debug("클라이언트 메시지 수신: eventId={}, userId={}, command={}", eventId, userId, command);

    return switch (command) {
      case "CANCEL" -> handleCancel(session, eventId, userId);
      default -> {
        log.warn("알 수 없는 명령어: {}", command);
        yield Mono.empty();
      }
    };
  }

  private Mono<Void> handleCancel(WebSocketSession session, Long eventId, Long userId) {
    return eventQueueService.deleteQueue(eventId, userId)
        .flatMap(removed -> {
          log.info("대기열 취소: eventId={}, userId={}, removed={}", eventId, userId, removed);
          return sendAndClose(session, QueueWebSocketResponse.cancelled(), CloseStatus.NORMAL);
        });
  }

  // ──────────────────────────────────────────────
  // 유틸리티
  // ──────────────────────────────────────────────

  private Mono<Void> sendAndClose(WebSocketSession session, QueueWebSocketResponse response,
                                  CloseStatus closeStatus) {
    return session.send(Mono.just(toWebSocketMessage(session, response)))
        .then(session.close(closeStatus));
  }

  private WebSocketMessage toWebSocketMessage(WebSocketSession session, QueueWebSocketResponse response) {
    try {
      return session.textMessage(objectMapper.writeValueAsString(response));
    } catch (JsonProcessingException e) {
      throw new RuntimeException("WebSocket 직렬화 실패", e);
    }
  }

  private boolean isType(WebSocketMessage msg, QueueMessageType type) {
    try {
      QueueWebSocketResponse parsed = objectMapper.readValue(
          msg.getPayloadAsText(), QueueWebSocketResponse.class);
      return type == parsed.type();
    } catch (Exception e) {
      return false;
    }
  }

  private Long extractEventId(WebSocketSession session) {
    try {
      String path = session.getHandshakeInfo().getUri().getPath();
      String[] segments = path.split("/");
      return Long.parseLong(segments[segments.length - 1]);
    } catch (Exception e) {
      return null;
    }
  }

  private Long extractUserId(WebSocketSession session) {
    try {
      String query = session.getHandshakeInfo().getUri().getQuery();
      if (query == null) return null;
      for (String param : query.split("&")) {
        String[] kv = param.split("=");
        if (kv.length == 2 && "userId".equals(kv[0])) {
          return Long.parseLong(kv[1]);
        }
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }
}