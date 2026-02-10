package org.example.ticketing.performance;

import org.example.ticketing.domain.reservation.dto.HoldResponseDto;
import org.example.ticketing.domain.reservation.exception.SeatAlreadyHeldException;
import org.example.ticketing.domain.reservation.service.ReservationHoldService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@SpringBootTest
@ActiveProfiles("test")
class SeatReservationConcurrencyTest {

  @Autowired
  ReservationHoldService reservationHoldService;

  @Autowired
  ReactiveRedisTemplate<String, String> redisTemplate;

  @Value("${reservation.hold-ttl}")
  private Duration configuredTtl;

  private static final int TOTAL_SEATS = 100;
  private static final int CONCURRENT_ATTEMPTS = 500;

  @BeforeEach
  void cleanRedis() {
    redisTemplate
        .getConnectionFactory()
        .getReactiveConnection()
        .serverCommands()
        .flushDb()
        .block();
  }

  @Test
  void 동일_좌석_동시_선점_테스트() {
    PerformanceTestReport report =
        new PerformanceTestReport("동일 좌석 동시 선점");

    AtomicInteger success = new AtomicInteger();
    AtomicInteger fail = new AtomicInteger();

    long start = System.currentTimeMillis();

    Flux.range(1, CONCURRENT_ATTEMPTS)
        .flatMap(i ->
                reservationHoldService.holdSeat(1L, (long) i)
                    .doOnNext(dto -> success.incrementAndGet())
                    .onErrorResume(e -> {
                      fail.incrementAndGet();
                      return Mono.empty();
                    })
            , CONCURRENT_ATTEMPTS)
        .then()
        .as(StepVerifier::create)
        .verifyComplete();

    long duration = System.currentTimeMillis() - start;

    report.addMetric("총 시도", CONCURRENT_ATTEMPTS);
    report.addMetric("성공", success.get());
    report.addMetric("실패", fail.get());
    report.addMetric("소요 시간(ms)", duration);
    report.addMetric("동시성 보장", success.get() == 1 ? "PASS" : "FAIL");

    report.saveReport();
  }

  @Test
  void 여러_좌석_동시_선점_테스트() {
    PerformanceTestReport report =
        new PerformanceTestReport("여러 좌석 동시 선점");

    AtomicInteger success = new AtomicInteger();
    AtomicInteger fail = new AtomicInteger();

    long start = System.currentTimeMillis();

    Flux.range(1, CONCURRENT_ATTEMPTS)
        .flatMap(i -> {
          long seatId = (i % TOTAL_SEATS) + 1;
          return reservationHoldService.holdSeat(seatId, (long) i)
              .doOnNext(dto -> success.incrementAndGet())
              .onErrorResume(e -> {
                fail.incrementAndGet();
                return Mono.empty();
              });
        }, 100)
        .then()
        .as(StepVerifier::create)
        .verifyComplete();

    long duration = System.currentTimeMillis() - start;

    report.addMetric("총 시도", CONCURRENT_ATTEMPTS);
    report.addMetric("좌석 수", TOTAL_SEATS);
    report.addMetric("성공", success.get());
    report.addMetric("실패", fail.get());
    report.addMetric("TPS", CONCURRENT_ATTEMPTS * 1000.0 / duration);

    report.saveReport();
  }

  @Test
  void 좌석_선점_TTL_만료_테스트() {
    PerformanceTestReport report =
        new PerformanceTestReport("좌석 선점 TTL 만료");

    report.addMetric("설정된 TTL", configuredTtl.getSeconds() + "초");

    HoldResponseDto first = null;
    try {
      first = reservationHoldService.holdSeat(1L, 1L).block();
      report.addMetric("첫 선점", "성공 ✓");
      report.addMetric("첫 선점 토큰", first.token());
    } catch (Exception e) {
      report.addMetric("첫 선점", "실패 ✗");
      report.saveReport();
      report.printConsole();
      throw e;
    }

    boolean secondFailed = false;
    try {
      reservationHoldService.holdSeat(1L, 2L).block();
    } catch (SeatAlreadyHeldException e) {
      secondFailed = true;
    }
    report.addMetric("즉시 재시도", secondFailed ? "차단됨 ✓" : "허용됨 ✗");

    long waitSeconds = configuredTtl.getSeconds() + 1;
    report.addMetric("대기 시간", waitSeconds + "초");

    try {
      Thread.sleep(waitSeconds * 1000);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    try {
      HoldResponseDto afterTtl = reservationHoldService.holdSeat(1L, 2L).block();
      report.addMetric("TTL 후 재시도", "성공 ✓");
      report.addMetric("새 토큰", afterTtl.token());
    } catch (Exception e) {
      report.addMetric("TTL 후 재시도", "실패 ✗ - " + e.getMessage());
    }

    report.saveReport();
    report.printConsole();
  }

  @Test
  void 대규모_동시_예매_시나리오_테스트() {
    PerformanceTestReport report =
        new PerformanceTestReport("대규모 동시 예매 시나리오");

    AtomicInteger seatHoldSuccess = new AtomicInteger();
    AtomicInteger seatHoldFail = new AtomicInteger();

    int USERS = 1000;
    long start = System.currentTimeMillis();

    Flux.range(1, USERS)
        .flatMap(i -> {
          long seatId = (i % TOTAL_SEATS) + 1;
          return reservationHoldService.holdSeat(seatId, (long) i)
              .onErrorReturn(HoldResponseDto.builder().seatId(null).token(null).build())
              .doOnNext(response -> {
                if (response != null && response.token() != null) {
                  seatHoldSuccess.incrementAndGet();
                } else {
                  seatHoldFail.incrementAndGet();
                }
              });
        }, 100)
        .then()
        .as(StepVerifier::create)
        .verifyComplete();

    long duration = System.currentTimeMillis() - start;

    report.addSection("테스트 결과");
    report.addMetric("전체 사용자", USERS);
    report.addMetric("총 좌석 수", TOTAL_SEATS);
    report.addMetric("좌석 선점 성공", seatHoldSuccess.get());
    report.addMetric("좌석 선점 실패", seatHoldFail.get());
    report.addMetric("성공률(%)",
        String.format("%.2f", seatHoldSuccess.get() * 100.0 / USERS));

    report.addSection("성능 지표");
    report.addMetric("총 소요 시간(ms)", duration);
    report.addMetric("평균 처리 시간(ms)",
        String.format("%.2f", duration / (double) USERS));
    report.addMetric("TPS",
        String.format("%.2f", USERS * 1000.0 / duration));

    report.addSection("분석");
    report.addMetric("예상 성공 수", TOTAL_SEATS + " (좌석 수)");
    report.addMetric("실제 성공 수", seatHoldSuccess.get());
    report.addMetric("동시성 제어",
        seatHoldSuccess.get() <= TOTAL_SEATS ? "정상 ✓" : "이상 ✗");

    report.saveReport();
    report.printConsole();
  }
}
