// 읽기 경로 베이스라인: GET /{id}/recommendations(저장된 결과 복원, 재계산 없이 SELECT만)와
// GET /saved만 두드린다. CommuteFilter/RecommendationScoreCalculator를 전혀 안 타므로,
// 01_write_heavy와 비교하면 "순수 DB 읽기 + Hikari 풀"만 놓고 본 대조군이 된다.
//
// setup()에서 테스트 유저별로 조건을 하나씩 미리 만들어두고(SEED_COUNT명), 본 실행은 그
// conditionId들을 랜덤하게 반복 조회한다. setup 단계 호출은 tags:{stage:'setup'}으로
// 구분해뒀으니 결과 필터링 시 제외하면 된다.
//
//   k6 run -e BASE_URL=http://localhost:8080 -e JWT_SECRET=<.env의 JWT_SECRET> \
//     loadtest/k6/scenarios/02_read_baseline.js

import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, TEST_USER_ID_MIN, TEST_USER_ID_MAX } from '../lib/config.js';
import { mintToken, authHeaders } from '../lib/auth.js';
import { buildConditionRequest } from '../lib/payloads.js';

const SEED_COUNT = Number(__ENV.SEED_COUNT || 20);

export const options = {
  scenarios: {
    read_baseline: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '1m', target: 10 },
        { duration: '30s', target: 50 },
        { duration: '1m', target: 50 },
        { duration: '30s', target: 100 },
        { duration: '1m', target: 100 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{endpoint:recommendations}': ['p(95)<1000'],
    'http_req_duration{endpoint:saved}': ['p(95)<1000'],
  },
};

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

    seeds.push({ userId, token, conditionId: res.json('data.conditionId') });
  }

  return { seeds };
}

export default function (data) {
  const seed = data.seeds[Math.floor(Math.random() * data.seeds.length)];
  const auth = authHeaders(seed.token);

  const recRes = http.get(
    `${BASE_URL}/api/user-conditions/${seed.conditionId}/recommendations`,
    Object.assign({}, auth, { tags: { endpoint: 'recommendations' } })
  );
  check(recRes, { 'recommendations: 200': (r) => r.status === 200 });

  const savedRes = http.get(
    `${BASE_URL}/api/user-conditions/saved`,
    Object.assign({}, auth, { tags: { endpoint: 'saved' } })
  );
  check(savedRes, { 'saved: 200': (r) => r.status === 200 });
}
