package org.example.ticketing.entity.health.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class HealthController {

  @RequestMapping("/health")
  public ResponseEntity<String> healthController() {
    return ResponseEntity.ok("Im good");
  }
}
