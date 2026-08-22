// 소크 테스트: 중간 부하(VU 10~20)를 30~60분 유지하면서 메모리/커넥션 누수나 장시간
// 성능 저하가 없는지 본다. 03_mixed와 같은 읽기/생성/재계산 혼합 비율을 쓰되, 램핑 없이
// constant-vus로 일정 부하를 오래 유지한다는 점만 다르다.
//
// 03_mixed/02_read_baseline은 setup()에서 JWT를 한 번만 발급해 seed에 저장해두고
// 재사용했는데, 소크는 30~60분 도니까 그 방식을 쓰면 기본 JWT 만료(1시간, JWT_EXPIRATION_MS)
// 근처에서 401이 나기 시작할 수 있다. 그래서 setup()은 userId/conditionId 쌍만 만들어두고,
// 매 iteration마다 mintToken()으로 그 자리에서 새로 서명한다(로컬 HMAC 계산이라 비용 거의
// 없음) - 테스트가 몇 시간을 돌아도 토큰 만료 걱정이 없다.
//
// 참고: 생성 비율(기본 20%)만큼 iteration마다 새 조건/추천이 계속 쌓인다 - 의도된
// 동작이고, 부하테스트 다 끝나면 아래로 정리 가능:
//   DELETE FROM user_conditions WHERE user_id IN
//     (SELECT user_id FROM users WHERE provider_id LIKE 'k6-loadtest-%');
//
//   k6 run -e BASE_URL=http://localhost:8080 -e JWT_SECRET=<.env의 JWT_SECRET> \
//     -e VUS=15 -e DURATION=30m loadtest/k6/scenarios/05_soak.js

import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, TEST_USER_ID_MIN, TEST_USER_ID_MAX } from '../lib/config.js';
import { mintToken, authHeaders } from '../lib/auth.js';
import { buildConditionRequest } from '../lib/payloads.js';

const SEED_COUNT = Number(__ENV.SEED_COUNT || 20);
const VUS = Number(__ENV.VUS || 15);
const DURATION = __ENV.DURATION || '30m';

// 누적 확률 경계 - READ_RATIO 미만이면 읽기, 그다음 CREATE_RATIO까지면 생성, 나머지는 재계산.
const READ_RATIO = Number(__ENV.READ_RATIO || 0.7);
const CREATE_RATIO_UPPER = READ_RATIO + Number(__ENV.CREATE_RATIO || 0.2); // 누적 0.9

export const options = {
  scenarios: {
    soak: {
      executor: 'constant-vus',
      vus: VUS,
      duration: DURATION,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{endpoint:recommendations}': ['p(95)<1000'],
    'http_req_duration{endpoint:saved}': ['p(95)<1000'],
    'http_req_duration{endpoint:create}': ['p(95)<5000'],
    'http_req_duration{endpoint:recompute}': ['p(95)<5000'],
  },
};

function randomUserId() {
  return TEST_USER_ID_MIN + Math.floor(Math.random() * (TEST_USER_ID_MAX - TEST_USER_ID_MIN + 1));
}

export function setup() {
  const poolSize = TEST_USER_ID_MAX - TEST_USER_ID_MIN + 1;
  const count = Math.min(SEED_COUNT, poolSize);
  const seeds = [];

  for (let i = 0; i < count; i++) {
    const userId = TEST_USER_ID_MIN + i;
    const token = mintToken(userId);
    const auth = authHeaders(token);
    const params = Object.assign({}, auth, { tags: { stage: 'setup' } });

    const res = http.post(`${BASE_URL}/api/user-conditions`, JSON.stringify(buildConditionRequest()), params);

    if (res.status !== 200 || !res.json('success')) {
      throw new Error(`setup 조건 생성 실패: userId=${userId}, status=${res.status}, body=${res.body}`);
    }

    // 토큰은 저장하지 않는다 - iteration마다 새로 발급해서 장시간 실행 중 만료를 피한다.
    seeds.push({ userId, conditionId: res.json('data.conditionId') });
  }

  return { seeds };
}

function doRead(seed) {
  const auth = authHeaders(mintToken(seed.userId));

  if (Math.random() < 0.5) {
    const res = http.get(
      `${BASE_URL}/api/user-conditions/${seed.conditionId}/recommendations`,
      Object.assign({}, auth, { tags: { endpoint: 'recommendations' } })
    );
    check(res, { 'recommendations: 200': (r) => r.status === 200 });
  } else {
    const res = http.get(
      `${BASE_URL}/api/user-conditions/saved`,
      Object.assign({}, auth, { tags: { endpoint: 'saved' } })
    );
    check(res, { 'saved: 200': (r) => r.status === 200 });
  }
}

function doCreate() {
  const auth = authHeaders(mintToken(randomUserId()));

  const res = http.post(
    `${BASE_URL}/api/user-conditions`,
    JSON.stringify(buildConditionRequest()),
    Object.assign({}, auth, { tags: { endpoint: 'create' } })
  );
  check(res, { 'create: 200': (r) => r.status === 200 });
}

function doRecompute(seed) {
  const auth = authHeaders(mintToken(seed.userId));

  const res = http.post(
    `${BASE_URL}/api/user-conditions/${seed.conditionId}/recompute`,
    null,
    Object.assign({}, auth, { tags: { endpoint: 'recompute' } })
  );
  check(res, { 'recompute: 200': (r) => r.status === 200 });
}

export default function (data) {
  const seed = data.seeds[Math.floor(Math.random() * data.seeds.length)];
  const r = Math.random();

  if (r < READ_RATIO) {
    doRead(seed);
  } else if (r < CREATE_RATIO_UPPER) {
    doCreate();
  } else {
    doRecompute(seed);
  }
}
