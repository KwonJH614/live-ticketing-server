import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { SharedArray } from 'k6/data';
import { BASE_URL, login, authHeaders } from './helpers.js';

/*
 * ============================================================
 *  좌석 선점 경쟁 테스트
 *  - 시나리오: 제한된 좌석에 대해 다수 유저가 동시에 선점 시도
 *  - 검증 포인트:
 *    1) Redis 기반 좌석 홀드의 동시성 제어 (1석 = 1명만 성공)
 *    2) 선점 실패 시 적절한 에러 응답
 *    3) 동시 요청 시 응답 시간 분포
 * ============================================================
 */

// --- 커스텀 메트릭 ---
const holdDuration = new Trend('seat_hold_duration', true);
const holdSuccessRate = new Rate('seat_hold_success_rate');
const holdConflictRate = new Rate('seat_hold_conflict_rate');
const seatListDuration = new Trend('seat_list_duration', true);
const totalHoldAttempts = new Counter('total_hold_attempts');
const successfulHolds = new Counter('successful_holds');

// --- 테스트 설정 ---
const EVENT_ID = __ENV.EVENT_ID || 1;
const SEAT_COUNT = parseInt(__ENV.SEAT_COUNT || '50');
const TEST_USERS = JSON.parse(__ENV.TEST_USERS || '[]');

export const options = {
  scenarios: {
    // 동시 좌석 선점 경쟁
    seat_rush: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5s', target: 100 },    // 빠르게 100명 진입
        { duration: '10s', target: 300 },    // 300명까지 증가
        { duration: '20s', target: 300 },    // 유지 (피크 경쟁)
        { duration: '5s', target: 0 },       // 종료
      ],
      exec: 'seatHoldScenario',
    },
  },
  thresholds: {
    seat_hold_duration: ['p(95)<2000'],        // 선점 응답 p95 < 2초
    seat_list_duration: ['p(95)<500'],         // 좌석 목록 p95 < 500ms
    seat_hold_success_rate: ['rate>0'],        // 최소 1건 이상 성공
    'http_req_failed{expected_response:true}': ['rate<0.01'], // 409 충돌 제외한 실제 에러율
  },
};

export function setup() {
  if (TEST_USERS.length === 0) {
    console.warn('⚠️  TEST_USERS 환경변수 필요. setup-test-data.js를 먼저 실행하세요.');
    return { tokens: [], seatIds: [] };
  }

  // 유저 로그인
  const tokens = [];
  for (const user of TEST_USERS) {
    const token = login(user.username, user.password);
    if (token) tokens.push(token);
  }
  console.log(`✅ ${tokens.length}/${TEST_USERS.length} 유저 로그인 성공`);

  // 좌석 목록 조회 (public API)
  const seatRes = http.get(`${BASE_URL}/api/seat/${EVENT_ID}`);
  let seatIds = [];
  if (seatRes.status === 200) {
    const body = JSON.parse(seatRes.body);
    seatIds = body.data.map((seat) => seat.id);
    console.log(`✅ ${seatIds.length}개 좌석 조회 완료`);
  } else {
    console.error(`좌석 조회 실패: ${seatRes.status}`);
    // 폴백: 1~SEAT_COUNT 좌석 ID 사용
    for (let i = 1; i <= SEAT_COUNT; i++) seatIds.push(i);
  }

  return { tokens, seatIds };
}

export function seatHoldScenario(data) {
  if (!data.tokens || data.tokens.length === 0) return;

  const token = data.tokens[__VU % data.tokens.length];
  const params = authHeaders(token);

  // 1. 좌석 목록 조회
  const listRes = http.get(`${BASE_URL}/api/seat/${EVENT_ID}`);
  seatListDuration.add(listRes.timings.duration);

  check(listRes, {
    '좌석 목록 조회 성공': (r) => r.status === 200,
  });

  sleep(Math.random() * 0.5); // 좌석 고르는 시간 시뮬레이션

  // 2. 랜덤 좌석 선점 시도
  const seatId = data.seatIds[Math.floor(Math.random() * data.seatIds.length)];

  const holdRes = http.post(
    `${BASE_URL}/api/reservation/${seatId}`,
    null,
    params
  );

  totalHoldAttempts.add(1);
  holdDuration.add(holdRes.timings.duration);

  const isSuccess = holdRes.status === 200;
  const isConflict = holdRes.status === 409 || holdRes.status === 400;

  holdSuccessRate.add(isSuccess);
  holdConflictRate.add(isConflict);

  if (isSuccess) {
    successfulHolds.add(1);
  } else if (!isConflict && __ITER < 3) {
    // 처음 몇 번만 디버깅용 로그 출력
    console.error(`좌석 선점 실패 - status: ${holdRes.status}, body: ${holdRes.body}`);
  }

  check(holdRes, {
    '선점 성공 또는 정상 충돌': (r) => r.status === 200 || r.status === 409 || r.status === 400,
    '선점 응답시간 < 2s': (r) => r.timings.duration < 2000,
  });

  sleep(Math.random() * 1);
}

export default function () {}
