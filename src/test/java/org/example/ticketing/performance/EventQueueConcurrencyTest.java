package org.example.ticketing.performance;

import org.example.ticketing.domain.event.service.EventQueueService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
class EventQueueConcurrencyTest {

  @Autowired
  EventQueueService eventQueueService;

  @Autowired
  ReactiveRedisTemplate<String, String> redisTemplate;

  private PerformanceTestReport report;

  private static final Long TEST_EVENT_ID = 1L;
  private static final int CONCURRENT_USERS = 1000;
  private static final int TOTAL_USERS = 5000;

  @BeforeEach
  void setUp(TestInfo testInfo) {
    redisTemplate.delete("event:queue:" + TEST_EVENT_ID).block();

    report = new PerformanceTestReport(testInfo.getDisplayName());
  }

  @AfterEach
  void tearDown() {
    report.saveReport();
  }

  @Test
  @DisplayName("대기열 동시 진입 부하 테스트")
  void 대기열_동시_진입_테스트() {
    long start = System.currentTimeMillis();
    AtomicInteger success = new AtomicInteger();
    AtomicInteger fail = new AtomicInteger();

    Flux.range(1, CONCURRENT_USERS)
        .flatMap(i ->
                eventQueueService.registerQueue(TEST_EVENT_ID, (long) i)
                    .doOnSuccess(v -> success.incrementAndGet())
                    .doOnError(e -> fail.incrementAndGet())
                    .onErrorResume(e -> Mono.empty()),
            CONCURRENT_USERS
        )
        .then()
        .as(StepVerifier::create)
        .verifyComplete();

    long duration = System.currentTimeMillis() - start;

    report.addSection("결과 요약");
    report.addMetric("동시 요청 수", CONCURRENT_USERS);
    report.addMetric("성공", success.get());
    report.addMetric("실패", fail.get());
    report.addMetric("소요 시간(ms)", duration);
    report.addMetric("TPS", CONCURRENT_USERS * 1000.0 / duration);
  }

  @Test
  @DisplayName("대기열 순번 조회 부하 테스트")
  void 대기열_순번_조회_부하_테스트() {
    Flux.range(1, 500)
        .flatMap(i -> eventQueueService.registerQueue(TEST_EVENT_ID, (long) i))
        .blockLast();

    long start = System.currentTimeMillis();
    AtomicInteger success = new AtomicInteger();

    Flux.range(1, CONCURRENT_USERS)
        .flatMap(i ->
                eventQueueService.getRank(TEST_EVENT_ID, (long) (i % 500 + 1))
                    .doOnSuccess(v -> success.incrementAndGet()),
            CONCURRENT_USERS
        )
        .then()
        .as(StepVerifier::create)
        .verifyComplete();

    long duration = System.currentTimeMillis() - start;

    report.addSection("결과 요약");
    report.addMetric("조회 요청 수", CONCURRENT_USERS);
    report.addMetric("성공 응답", success.get());
    report.addMetric("소요 시간(ms)", duration);
    report.addMetric("TPS", CONCURRENT_USERS * 1000.0 / duration);
  }

  @Test
  @DisplayName("대기열 진입 + 순번 조회 혼합 부하 테스트")
  void 대기열_진입_및_순번_조회_혼합_부하_테스트() {
    long start = System.currentTimeMillis();
    AtomicInteger joinCount = new AtomicInteger();
    AtomicInteger rankCount = new AtomicInteger();

    Flux.range(1, TOTAL_USERS)
        .flatMap(i -> {
          if (i % 10 < 7) {
            return eventQueueService.registerQueue(TEST_EVENT_ID, (long) i)
                .doOnSuccess(v -> joinCount.incrementAndGet())
                .onErrorResume(e -> Mono.empty());
          } else {
            return eventQueueService.getRank(TEST_EVENT_ID, (long) (i % 1000 + 1))
                .doOnSuccess(v -> rankCount.incrementAndGet())
                .onErrorResume(e -> Mono.empty());
          }
        }, CONCURRENT_USERS)
        .then()
        .as(StepVerifier::create)
        .verifyComplete();

    long duration = System.currentTimeMillis() - start;

    report.addSection("결과 요약");
    report.addMetric("총 요청 수", TOTAL_USERS);
    report.addMetric("대기열 진입 요청", joinCount.get());
    report.addMetric("순번 조회 요청", rankCount.get());
    report.addMetric("소요 시간(ms)", duration);
    report.addMetric("TPS", TOTAL_USERS * 1000.0 / duration);
  }
}
