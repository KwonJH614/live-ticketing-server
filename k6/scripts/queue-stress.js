import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';
import { BASE_URL, login, authHeaders } from './helpers.js';

/*
 * ============================================================
 *  대기열 스트레스 테스트
 *  - 시나리오: 수백 명의 유저가 동시에 대기열에 진입하고 순번을 조회
 *  - 검증 포인트:
 *    1) Redis ZSet 기반 대기열의 동시 쓰기 성능
 *    2) 순번 조회 응답 시간 (p95 < 500ms 목표)
 *    3) 대기열 진입 실패율
 * ============================================================
 */

// --- 커스텀 메트릭 ---
const queueRegisterDuration = new Trend('queue_register_duration', true);
const queueStatusDuration = new Trend('queue_status_duration', true);
const queueRegisterFailRate = new Rate('queue_register_fail_rate');
const queueStatusFailRate = new Rate('queue_status_fail_rate');
const totalRequests = new Counter('total_requests');

// --- 테스트 설정 ---
const EVENT_ID = __ENV.EVENT_ID || 1;
const TEST_USERS = JSON.parse(__ENV.TEST_USERS || '[]');

export const options = {
  scenarios: {
    // 1단계: 램프업 - 점진적으로 유저 증가
    ramp_up: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '10s', target: 50 },   // 10초 동안 50명까지
        { duration: '20s', target: 200 },   // 20초 동안 200명까지
        { duration: '30s', target: 500 },   // 30초 동안 500명까지 (피크)
        { duration: '20s', target: 500 },   // 20초 유지
        { duration: '10s', target: 0 },     // 10초 동안 종료
      ],
      exec: 'queueScenario',
    },
  },
  thresholds: {
    queue_register_duration: ['p(95)<1000'],  // 대기열 등록 p95 < 1초
    queue_status_duration: ['p(95)<500'],     // 순번 조회 p95 < 500ms
    queue_register_fail_rate: ['rate<0.05'],  // 등록 실패율 < 5%
    queue_status_fail_rate: ['rate<0.05'],    // 조회 실패율 < 5%
  },
};

// --- setup: 테스트 유저 로그인 ---
export function setup() {
  if (TEST_USERS.length === 0) {
    console.warn('⚠️  TEST_USERS 환경변수가 설정되지 않았습니다.');
    console.warn('   사전에 테스트 유저를 DB에 생성한 후 다음과 같이 실행하세요:');
    console.warn('   k6 run -e TEST_USERS=\'[{"username":"user1","password":"pass1"},...]\'');
    console.warn('   또는 setup-test-data.js를 먼저 실행하세요.');
    return { tokens: [] };
  }

  const tokens = [];
  for (const user of TEST_USERS) {
    const token = login(user.username, user.password);
    if (token) {
      tokens.push(token);
    }
  }

  console.log(`✅ ${tokens.length}/${TEST_USERS.length} 유저 로그인 성공`);
  return { tokens };
}

// --- 메인 시나리오 ---
export function queueScenario(data) {
  if (!data.tokens || data.tokens.length === 0) {
    console.error('토큰 없음 - setup 실패');
    return;
  }

  // VU별로 다른 토큰 사용 (라운드 로빈)
  const token = data.tokens[__VU % data.tokens.length];
  const params = authHeaders(token);

  // 1. 대기열 진입
  const registerRes = http.post(
    `${BASE_URL}/api/queue/${EVENT_ID}`,
    null,
    params
  );

  totalRequests.add(1);
  queueRegisterDuration.add(registerRes.timings.duration);
  queueRegisterFailRate.add(registerRes.status !== 200);

  check(registerRes, {
    '대기열 등록 성공 (200)': (r) => r.status === 200,
    '대기열 등록 응답시간 < 1s': (r) => r.timings.duration < 1000,
  });

  sleep(0.5); // 실제 유저처럼 약간의 딜레이

  // 2. 대기열 순번 조회 (폴링 시뮬레이션 - 3회)
  for (let i = 0; i < 3; i++) {
    const statusRes = http.get(
      `${BASE_URL}/api/queue/${EVENT_ID}`,
      params
    );

    totalRequests.add(1);
    queueStatusDuration.add(statusRes.timings.duration);
    queueStatusFailRate.add(statusRes.status !== 200);

    check(statusRes, {
      '순번 조회 성공 (200)': (r) => r.status === 200,
      '순번 조회 응답시간 < 500ms': (r) => r.timings.duration < 500,
    });

    if (statusRes.status === 200) {
      const body = JSON.parse(statusRes.body);
      if (body.data && body.data.isAvailable) {
        break; // 통과 가능하면 폴링 중단
      }
    }

    sleep(1); // 1초 간격 폴링
  }

  sleep(Math.random() * 2); // VU 간 분산
}

export default function () {
  // ramping-vus의 기본 함수 (scenarios에서 exec 지정했으므로 사용 안됨)
}
