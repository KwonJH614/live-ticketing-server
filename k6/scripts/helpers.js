import http from 'k6/http';

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

/**
 * 로그인 후 JWT 토큰 반환
 */
export function login(username, password) {
  const res = http.post(
    `${BASE_URL}/api/users/login`,
    JSON.stringify({ username, password }),
    { headers: { 'Content-Type': 'application/json' } }
  );

  if (res.status !== 200) {
    console.error(`Login failed for ${username}: ${res.status} ${res.body}`);
    return null;
  }

  const body = JSON.parse(res.body);
  return body.data.token;
}

/**
 * 인증 헤더 생성
 */
export function authHeaders(token) {
  return {
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
  };
}
