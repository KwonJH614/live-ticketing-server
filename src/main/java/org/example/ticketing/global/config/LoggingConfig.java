package org.example.ticketing.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.server.WebFilter;

@Configuration
public class LoggingConfig {

  @Bean
  public WebFilter logFilter() {
    return (exchange, chain) -> {
      System.out.println("Request path : " + exchange.getRequest().getPath());
      return chain.filter(exchange);
    };
  }
}
