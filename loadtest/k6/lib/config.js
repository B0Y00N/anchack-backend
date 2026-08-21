// 공통 설정. 전부 -e 플래그(또는 환경변수)로 넘긴다.
//   k6 run -e BASE_URL=http://localhost:8080 -e JWT_SECRET=... loadtest/k6/scenarios/00_smoke.js
//
// JWT_SECRET은 .env의 JWT_SECRET과 반드시 동일해야 한다 - 앱이 그 값을 UTF-8 바이트로
// HMAC 서명 검증에 그대로 쓰기 때문이다(JwtTokenProvider 참고).
//
// TEST_USER_ID_MIN/MAX는 loadtest/README.md의 seed_k6_test_users.sql로 심어둔
// users.user_id 범위다(기본 4~53, provider_id='k6-loadtest-001'~'050').

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

export const JWT_SECRET = __ENV.JWT_SECRET;

if (!JWT_SECRET) {
  throw new Error(
    'JWT_SECRET이 없습니다. -e JWT_SECRET=<.env의 JWT_SECRET 값>으로 실행하세요.'
  );
}

export const TEST_USER_ID_MIN = Number(__ENV.TEST_USER_ID_MIN || 4);
export const TEST_USER_ID_MAX = Number(__ENV.TEST_USER_ID_MAX || 53);

export const JWT_EXPIRATION_MS = Number(__ENV.JWT_EXPIRATION_MS || 3600000);
