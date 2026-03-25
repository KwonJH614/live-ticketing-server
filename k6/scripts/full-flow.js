import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import ws from 'k6/ws';
import { BASE_URL, login, authHeaders } from './helpers.js';

/*
 * ============================================================
 *  전체 티켓팅 플로우 부하 테스트
 *  - 시나리오: 실제 티켓팅과 동일한 E2E 플로우
 *    로그인 → 이벤트 목록 → 대기열 진입 → WebSocket 대기
 *    → 이벤트 상세 → 좌석 조회 → 좌석 선점
 *  - 검증 포인트:
 *    1) 전체 플로우의 응답 시간 분포
 *    2) 각 단계별 병목 구간 식별
 *    3) WebSocket 기반 대기열 실시간 push 성능
 * ============================================================
 */

// --- 커스텀 메트릭 ---
const loginDuration = new Trend('flow_login_duration', true);
const eventListDuration = new Trend('flow_event_list_duration', true);
const queueRegDuration = new Trend('flow_queue_register_duration', true);
const queueWaitDuration = new Trend('flow_queue_wait_duration', true);
const eventDetailDuration = new Trend('flow_event_detail_duration', true);
const seatListDuration = new Trend('flow_seat_list_duration', true);
const seatHoldDuration = new Trend('flow_seat_hold_duration', true);
const flowCompletionRate = new Rate('flow_completion_rate');
const flowTotalDuration = new Trend('flow_total_duration', true);
const wsConnectionRate = new Rate('ws_connection_success_rate');

// --- 테스트 설정 ---
const EVENT_ID = __ENV.EVENT_ID || 1;
const TEST_USERS = JSON.parse(__ENV.TEST_USERS || '[]');
const WS_BASE_URL = (BASE_URL).replace('http://', 'ws://').replace('https://', 'wss://');
const WS_TIMEOUT = parseInt(__ENV.WS_TIMEOUT || '30'); // WebSocket 대기 최대 시간(초)

export const options = {
  scenarios: {
    ticket_open: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5s', target: 20 },     // 워밍업
        { duration: '10s', target: 100 },    // 초기 진입
        { duration: '15s', target: 500 },    // 피크 (티켓 오픈 직후)
        { duration: '30s', target: 500 },    // 피크 유지
        { duration: '10s', target: 100 },    // 감소
        { duration: '5s', target: 0 },       // 종료
      ],
      exec: 'ticketingFlow',
    },
  },
  thresholds: {
    flow_login_duration: ['p(95)<2000'],
    flow_event_list_duration: ['p(95)<1000'],
    flow_queue_register_duration: ['p(95)<1000'],
    flow_queue_wait_duration: ['med<5000'],         // WebSocket 대기 중앙값 < 5초
    flow_event_detail_duration: ['p(95)<1000'],
    flow_seat_list_duration: ['p(95)<500'],
    flow_seat_hold_duration: ['p(95)<2000'],
    flow_total_duration: ['med<10000'],             // 전체 플로우 중앙값 < 10초
    flow_completion_rate: ['rate>0'],               // 최소 1건 이상 좌석 선점 성공
    ws_connection_success_rate: ['rate>0.8'],       // WebSocket 연결 성공률 > 80%
  },
};

export function setup() {
  if (TEST_USERS.length === 0) {
    console.warn('⚠️  TEST_USERS 환경변수 필요');
    return { users: [] };
  }
  return { users: TEST_USERS };
}

export function ticketingFlow(data) {
  if (!data.users || data.users.length === 0) return;

  const user = data.users[__VU % data.users.length];
  const flowStart = Date.now();
  let completed = false;

  // ── Step 1: 로그인 ──
  let token;
  group('01_로그인', () => {
    const res = http.post(
      `${BASE_URL}/api/users/login`,
      JSON.stringify({ username: user.username, password: user.password }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    loginDuration.add(res.timings.duration);

    if (
      !check(res, {
        '로그인 성공': (r) => r.status === 200,
      })
    ) {
      return;
    }

    const body = JSON.parse(res.body);
    token = body.data.token;
  });

  if (!token) {
    flowCompletionRate.add(false);
    return;
  }

  const params = authHeaders(token);
  sleep(0.3);

  // ── Step 2: 이벤트 목록 조회 ──
  group('02_이벤트_목록', () => {
    const res = http.get(`${BASE_URL}/api/events?page=0&size=10`);
    eventListDuration.add(res.timings.duration);
    check(res, { '이벤트 목록 조회 성공': (r) => r.status === 200 });
  });

  sleep(0.5 + Math.random() * 1); // 이벤트 고르는 시간

  // ── Step 3: 대기열 진입 ──
  group('03_대기열_진입', () => {
    const res = http.post(`${BASE_URL}/api/queue/${EVENT_ID}`, null, params);
    queueRegDuration.add(res.timings.duration);
    check(res, { '대기열 등록 성공': (r) => r.status === 200 });
  });

  sleep(0.3);

  // ── Step 4: WebSocket으로 대기열 상태 수신 ──
  let hasAccess = false;
  group('04_WebSocket_대기열', () => {
    const wsUrl = `${WS_BASE_URL}/ws/queue/${EVENT_ID}?token=${encodeURIComponent(token)}`;
    const wsStart = Date.now();

    const res = ws.connect(wsUrl, {}, function (socket) {
      wsConnectionRate.add(true);

      socket.on('message', (msg) => {
        try {
          const data = JSON.parse(msg);

          if (data.type === 'AVAILABLE' && data.isAvailable) {
            hasAccess = true;
            queueWaitDuration.add(Date.now() - wsStart);
            socket.close();
          }
          // RANK_UPDATE는 계속 대기
        } catch (e) {
          // JSON 파싱 실패 무시
        }
      });

      socket.on('error', () => {
        wsConnectionRate.add(false);
        socket.close();
      });

      // 최대 대기 시간 후 타임아웃
      socket.setTimeout(function () {
        if (!hasAccess) {
          queueWaitDuration.add(Date.now() - wsStart);
          socket.close();
        }
      }, WS_TIMEOUT * 1000);
    });

    if (!hasAccess) {
      // WebSocket 연결 자체가 실패한 경우
      if (!res || res.status !== 101) {
        wsConnectionRate.add(false);
      }
    }
  });

  if (!hasAccess) {
    flowCompletionRate.add(false);
    flowTotalDuration.add(Date.now() - flowStart);
    return;
  }

  // ── Step 5: 이벤트 상세 조회 ──
  group('05_이벤트_상세', () => {
    const res = http.get(`${BASE_URL}/api/events/${EVENT_ID}`, params);
    eventDetailDuration.add(res.timings.duration);
    check(res, { '이벤트 상세 조회 성공': (r) => r.status === 200 });
  });

  sleep(0.3 + Math.random() * 0.5);

  // ── Step 6: 좌석 목록 조회 ──
  let seatIds = [];
  group('06_좌석_조회', () => {
    const res = http.get(`${BASE_URL}/api/seat/${EVENT_ID}`);
    seatListDuration.add(res.timings.duration);

    if (
      check(res, { '좌석 목록 조회 성공': (r) => r.status === 200 })
    ) {
      const body = JSON.parse(res.body);
      seatIds = body.data
        .filter((s) => !s.reserved)
        .map((s) => s.id);
    }
  });

  if (seatIds.length === 0) {
    flowCompletionRate.add(false);
    flowTotalDuration.add(Date.now() - flowStart);
    return;
  }

  sleep(0.5 + Math.random() * 1); // 좌석 고르는 시간

  // ── Step 7: 좌석 선점 ──
  group('07_좌석_선점', () => {
    const seatId = seatIds[Math.floor(Math.random() * seatIds.length)];
    const res = http.post(`${BASE_URL}/api/reservation/${seatId}`, null, params);
    seatHoldDuration.add(res.timings.duration);

    check(res, {
      '좌석 선점 성공 또는 충돌': (r) =>
        r.status === 200 || r.status === 409 || r.status === 400,
    });

    if (res.status === 200) {
      completed = true;
    }
  });

  flowCompletionRate.add(completed);
  flowTotalDuration.add(Date.now() - flowStart);
}

export default function () {}
