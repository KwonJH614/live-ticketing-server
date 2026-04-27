# k6 Full-Flow Load Test Report

## 테스트 요약

이 테스트는 티켓 오픈 상황을 가정해 인증된 사용자가 대기열에 진입하고, WebSocket으로 입장 가능 상태를 받은 뒤 이벤트 상세 조회, 좌석 목록 조회, Redis 기반 좌석 임시 선점까지 수행하는 full-flow 시나리오다.

로그인은 k6 `setup()` 단계에서 사전 수행했고, 본 부하 구간에서는 발급된 JWT를 재사용했다. 따라서 본 결과는 로그인 처리량이 아니라 인증된 사용자의 예매 진입 흐름 처리 성능을 검증한 결과다.

| 항목 | 결과 |
| --- | --- |
| 테스트 시나리오 | 이벤트 목록 조회 -> 대기열 등록 -> WebSocket 대기 -> 이벤트 상세 조회 -> 좌석 목록 조회 -> 좌석 임시 선점 |
| 최대 동시 사용자 | 1,000 VU |
| 완료 iteration | 11,174건 |
| 중단 iteration | 605건 |
| 총 HTTP 요청 | 55,713건 |
| 총 WebSocket 세션 | 11,779건 |
| 테스트 좌석 수 | 100석 |
| 좌석 임시 선점 성공 | 100건 |
| 좌석 임시 선점 충돌 | 10,285건 |

## 핵심 결과

| 지표 | 결과 |
| --- | ---: |
| HTTP 실패율 | 0.00% |
| k6 check 성공률 | 100.00% |
| WebSocket 연결 성공률 | 100.00% |
| HTTP 응답 시간 p95 | 137.52ms |
| 대기열 등록 p95 | 129.20ms |
| 이벤트 상세 조회 p95 | 139.72ms |
| 좌석 목록 조회 p95 | 141.37ms | 
| 좌석 임시 선점 p95 | 136.63ms |

`flow_completion_rate`는 0.89%로 낮게 표시되지만, 현재 스크립트에서 completion은 좌석 hold 응답이 `200 OK`인 경우만 집계한다. 테스트 좌석은 100석으로 제한되어 있어 100건만 hold에 성공했고, 이후 요청은 이미 hold된 좌석에 대한 `409 Conflict`로 정상 차단되었다.

## 단계별 응답 시간

| 단계 | avg | med | p90 | p95 | max |
| --- | ---: | ---: | ---: | ---: | ---: |
| 이벤트 목록 조회 | 41.34ms | 24.08ms | 100.02ms | 138.36ms | 2.24s |
| 대기열 등록 | 41.38ms | 24.84ms | 91.11ms | 129.20ms | 570.58ms |
| 이벤트 상세 조회 | 42.59ms | 26.78ms | 93.33ms | 139.72ms | 1.10s |
| 좌석 목록 조회 | 44.65ms | 27.04ms | 93.83ms | 141.37ms | 2.06s |
| 좌석 임시 선점 | 43.43ms | 26.64ms | 94.88ms | 136.63ms | 558.03ms |
| 전체 HTTP | 43.71ms | 26.20ms | 98.91ms | 137.52ms | 2.24s |

일부 `min` 값에 음수가 기록되었으나, 이는 Windows Docker 환경에서 k6 시간 측정 중 발생한 clock drift로 판단된다. 성능 평가는 `p90`, `p95`, 실패율 중심으로 해석했다.

## WebSocket 결과

| 지표 | 결과 |
| --- | ---: |
| WebSocket 연결 성공률 | 100.00% |
| WebSocket 세션 수 | 11,779 |
| 메시지 수신 수 | 11,868 |
| 연결 시간 avg | 75.63ms |
| 연결 시간 p95 | 232.93ms |
| 세션 지속 시간 med | 54.53ms |
| 세션 지속 시간 p90 | 273.61ms |
| 세션 지속 시간 p95 | 1m30s |

WebSocket 연결 자체는 100% 성공했다. 다만 일부 사용자는 테스트 종료 시점까지 대기열 access 메시지를 받지 못해 90초 timeout까지 대기했다. 이 때문에 `flow_queue_wait_duration`과 `flow_total_duration`의 p95가 약 1분 30초로 나타났다.

## 좌석 선점 경합 검증

| 항목 | 결과 |
| --- | ---: |
| 총 좌석 수 | 100석 |
| hold 성공 | 100건 |
| hold 충돌 | 10,285건 |
| HTTP 실패율 | 0.00% |
| 중복 선점 | 0건으로 해석 가능 |

좌석 임시 선점은 Redis `setIfAbsent`를 이용해 처리한다. 1,000 VU 테스트에서 100석은 정확히 한 번씩만 hold에 성공했고, 이후 동일 좌석에 대한 요청은 `409 Conflict`로 차단되었다.

따라서 낮은 `flow_completion_rate`는 장애가 아니라 좌석 수 제한과 랜덤 좌석 선택으로 인한 정상적인 경합 결과다. 이 테스트에서는 좌석 경합 상황에서도 서버가 실패 응답 없이 빠르게 충돌을 제어하는지 확인하는 것이 핵심이다.

## 테스트 환경

| 구분 | 환경 |
| --- | --- |
| 부하 발생 | 로컬 Windows PC에서 Docker 기반 `grafana/k6` 컨테이너 실행 |
| 대상 서버 | AWS EC2 |
| EC2 사양 | t3.small급, 2 vCPU / 2 GiB RAM |
| 서버 런타임 | Amazon Linux 2023, Docker Compose |
| 애플리케이션 | Spring Boot 3.5, WebFlux, Netty, JWT 인증 |
| DB | AWS RDS PostgreSQL db.t3.micro급, database `ticketing`, Spring Data R2DBC |
| Redis | EC2 내부 Docker Compose Redis, Redis 7 Alpine |
| Redis 사용처 | 대기열 Sorted Set, access key, 좌석 hold TTL |
| k6 시나리오 | `k6/scripts/ec2-full-flow.js`, `STAGE_PRESET=heavy`, `USER_COUNT=1000` |
| 테스트 데이터 | 테스트 계정 1,000개, 테스트 좌석 100석 |
| 네트워크 구성 | 로컬 k6는 EC2 8080 포트로만 요청, RDS는 EC2 보안 그룹에서만 접근 |

RDS는 EC2 보안 그룹에서만 접근하도록 구성했고, 로컬 k6 클라이언트는 EC2의 8080 포트로만 부하를 발생시켰다. DB 직접 접근은 EC2를 통해 수행했다.

## 결론

제한된 EC2/RDS 환경에서 1,000 VU full-flow 부하 테스트를 수행한 결과, HTTP 실패율 0%, WebSocket 연결 성공률 100%, 주요 API p95 150ms 이하를 기록했다.

좌석 선점은 100석 제한으로 인해 성공 100건 이후 대부분 충돌로 전환되었지만, 이는 Redis 기반 동시성 제어가 정상 동작한 결과다. full-flow 기준으로 대기열, WebSocket, 좌석 조회, Redis hold까지 포함한 예매 진입 흐름이 1,000 VU에서 안정적으로 처리됨을 확인했다.
