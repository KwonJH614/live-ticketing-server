package org.example.ticketing.global.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;

@Configuration
public class LoggingConfig {

  private static final Logger log = LoggerFactory.getLogger("RequestLogger");

  @Bean
  public WebFilter logFilter() {
    return (ServerWebExchange exchange, WebFilterChain chain) -> {
      String method = exchange.getRequest().getMethod().name();
      String path = exchange.getRequest().getPath().value();
      log.info(">>> Incoming request: {} {}", method, path);

      return chain.filter(exchange)
              .doOnSuccess(done -> {
                log.info("<<< Response status: {} for {} {}",
                        exchange.getResponse().getStatusCode(), method, path);
              });
    };
  }
}
