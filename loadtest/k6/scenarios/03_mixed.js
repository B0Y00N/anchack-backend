// 혼합 시나리오: 읽기 70% / 생성 20% / 재계산 10% 비율로 실제 트래픽 패턴을 흉내낸다.
// 01_write_heavy/02_read_baseline은 각 경로를 단독으로 몰아붙이는 대조군이었고, 이건 그
// 둘을 하나의 부하 아래 동시에 섞어서 - 예를 들어 읽기 트래픽이 많을 때 create/recompute의
// 데드락 재시도(DeadlockRetry)가 커넥션 풀/레이턴시에 어떤 영향을 주는지 - 같이 본다.
//
// setup()에서 SEED_COUNT명의 조건을 미리 만들어두고, 읽기/재계산은 그 seed를 재사용한다.
// 생성만 매 iteration마다 새 조건을 만든다(그래야 read_baseline과 달리 DB에 계속 새
// recommendations가 쌓이는 실제 트래픽에 가까운 그림이 됨).
//
//   k6 run -e BASE_URL=http://localhost:8080 -e JWT_SECRET=<.env의 JWT_SECRET> \
//     loadtest/k6/scenarios/03_mixed.js

import http from 'k6/http';
import { check } from 'k6';
import { BASE_URL, TEST_USER_ID_MIN, TEST_USER_ID_MAX } from '../lib/config.js';
import { mintToken, authHeaders } from '../lib/auth.js';
import { buildConditionRequest } from '../lib/payloads.js';

const SEED_COUNT = Number(__ENV.SEED_COUNT || 20);

// 누적 확률 경계 - READ_RATIO 미만이면 읽기, 그다음 CREATE_RATIO까지면 생성, 나머지는 재계산.
const READ_RATIO = Number(__ENV.READ_RATIO || 0.7);
const CREATE_RATIO_UPPER = READ_RATIO + Number(__ENV.CREATE_RATIO || 0.2); // 누적 0.9

export const options = {
  scenarios: {
    mixed: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 10 },
        { duration: '1m', target: 10 },
        { duration: '30s', target: 20 },
        { duration: '1m', target: 20 },
        { duration: '30s', target: 30 },
        { duration: '1m', target: 30 },
        { duration: '30s', target: 0 },
      ],
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

    seeds.push({ userId, token, conditionId: res.json('data.conditionId') });
  }

  return { seeds };
}

function doRead(seed) {
  const auth = authHeaders(seed.token);

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
  const auth = authHeaders(seed.token);

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
