import lombok.RequiredArgsConstructor;
import org.example.ticketing.domain.Queue.handler.EventQueueWebSocketHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;

import java.util.HashMap;
import java.util.Map;

@Configuration
@RequiredArgsConstructor
public class WebSocketConfig {
  private final EventQueueWebSocketHandler eventQueueWebSocketHandler;

  @Bean
  public HandlerMapping webSocketHandlerMapping() {
    Map<String, WebSocketHandler> map = new HashMap<>();
    map.put("/ws/queue/{eventId}", eventQueueWebSocketHandler);

    SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
    handlerMapping.setUrlMap(map);
    handlerMapping.setOrder(-1);
    return handlerMapping;
  }

  @Bean
  public WebSocketHandlerAdapter handlerAdapter() {
    return new WebSocketHandlerAdapter();
  }
}