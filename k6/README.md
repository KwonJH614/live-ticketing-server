# k6 부하 테스트

실제 부하 상황에서의 시스템 성능을 검증하는 k6 스크립트입니다.

## 사전 준비

### 1. k6 설치

```bash
# Windows (choco)
choco install k6

# Windows (winget)
winget install grafana.k6

# macOS
brew install k6
```

### 2. 서버 실행

PostgreSQL, Redis가 실행 중이어야 합니다.

```bash
docker-compose up -d     # Redis
./gradlew bootRun        # Spring Boot 서버
```

### 3. 테스트 데이터 생성

**방법 A: SQL 직접 실행** (권장)

```bash
# 1. 먼저 BCrypt 해시값 확인 (서버 로그 또는 DB에서 기존 유저 해시 복사)
# 2. setup-test-data.sql의 PLACEHOLDER_HASH_REPLACE_ME를 실제 해시로 교체
# 3. SQL 실행
psql -d ticketing -f k6/scripts/setup-test-data.sql
```

**방법 B: k6 스크립트로 생성** (이메일 인증 우회 필요)

```bash
k6 run k6/scripts/setup-test-data.js
```

### 4. TEST_USERS JSON 준비

```bash
# 100명 유저 JSON 생성 (bash)
echo '[' > /tmp/users.json
for i in $(seq 1 100); do
  [ $i -gt 1 ] && echo ',' >> /tmp/users.json
  echo "{\"username\":\"loadtest_user_$i\",\"password\":\"Qwer1234\"}" >> /tmp/users.json
done
echo ']' >> /tmp/users.json
```

## 테스트 실행

### 대기열 스트레스 테스트

수백 명이 동시에 대기열 진입 + 순번 조회를 반복하는 시나리오입니다.

```bash
k6 run \
  -e EVENT_ID=1 \
  -e TEST_USERS="$(cat /tmp/users.json)" \
  k6/scripts/queue-stress.js
```

### 좌석 선점 경쟁 테스트

제한된 좌석에 대해 다수 유저가 동시 선점을 시도하는 시나리오입니다.

```bash
k6 run \
  -e EVENT_ID=3 \
  -e TEST_USERS="$(cat /tmp/users.json)" \
  k6/scripts/seat-hold-stress.js
```

### 전체 티켓팅 플로우 테스트

로그인 → 대기열 → 좌석 선점까지 E2E 시나리오입니다.

```bash
k6 run \
  -e EVENT_ID=3 \
  -e TEST_USERS="$(cat /tmp/users.json)" \
  k6/scripts/full-flow.js
```

## 주요 성능 지표 (Thresholds)

| 지표 | 목표 | 설명 |
|------|------|------|
| `queue_register_duration p95` | < 1s | 대기열 등록 응답 시간 |
| `queue_status_duration p95` | < 500ms | 순번 조회 응답 시간 |
| `seat_hold_duration p95` | < 2s | 좌석 선점 응답 시간 |
| `flow_total_duration p95` | < 15s | 전체 플로우 소요 시간 |
| `queue_register_fail_rate` | < 5% | 대기열 등록 실패율 |
| `flow_completion_rate` | > 50% | 전체 플로우 완료율 |

## 결과 저장

```bash
# JSON 리포트 출력
k6 run --out json=k6/results/queue-stress-result.json k6/scripts/queue-stress.js

# HTML 리포트 (k6-reporter 플러그인 필요)
k6 run --out json=k6/results/result.json k6/scripts/full-flow.js
```

## 부하 조정

각 스크립트의 `options.scenarios.*.stages`를 수정하여 부하량을 조절할 수 있습니다.

```javascript
stages: [
  { duration: '10s', target: 50 },   // 목표 VU 수 조절
  { duration: '30s', target: 1000 },  // 더 높은 부하
  { duration: '60s', target: 1000 },  // 더 긴 유지 시간
  { duration: '10s', target: 0 },
]
```
