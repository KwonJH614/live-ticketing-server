import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL } from './helpers.js';

/*
 * ============================================================
 *  테스트 데이터 생성 스크립트
 *  - k6 부하 테스트에 필요한 테스트 유저를 일괄 생성
 *  - 실행: k6 run k6/scripts/setup-test-data.js
 *  - 주의: 이메일 인증이 활성화되어 있으면 DB에 직접 INSERT 필요
 * ============================================================
 */

const USER_COUNT = parseInt(__ENV.USER_COUNT || '100');
const PASSWORD = __ENV.PASSWORD || 'Test1234!';

export const options = {
  vus: 1,
  iterations: 1,
};

export default function () {
  const users = [];

  for (let i = 1; i <= USER_COUNT; i++) {
    const username = `loadtest_user_${i}`;
    const email = `loadtest${i}@test.com`;

    const res = http.post(
      `${BASE_URL}/api/users/register`,
      JSON.stringify({
        username: username,
        password: PASSWORD,
        email: email,
        verificationCode: '000000', // 테스트 환경에서 인증 코드 우회 필요
      }),
      { headers: { 'Content-Type': 'application/json' } }
    );

    if (res.status === 200) {
      users.push({ username, password: PASSWORD });
      if (i % 20 === 0) console.log(`${i}/${USER_COUNT} 유저 생성 완료`);
    } else if (i <= 3) {
      // 처음 몇 개만 에러 출력
      console.warn(`유저 생성 실패 (${username}): ${res.status} ${res.body}`);
    }
  }

  console.log(`\n✅ 총 ${users.length}/${USER_COUNT} 유저 생성 완료`);
  console.log('\n아래 JSON을 TEST_USERS 환경변수로 사용하세요:');
  console.log(JSON.stringify(users));
}
