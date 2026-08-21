// 핵심 쓰기 경로 부하테스트: POST /user-conditions(생성) 후 바로 POST /{id}/recompute(재계산).
// 이 도메인에서 가장 무거운 경로 - HardFilter(구/필수인프라/예산·주거유형/통근) ->
// RecommendationScoreCalculator(9개 지표 테이블 조인, 캐시 없음) -> reason/caution 생성을
// 매번 그대로 탄다.
//
// 기본은 destAddress 없이(guCodes 기반) 보내 CommuteFilter/RouteService 자체를 건너뛰고
// 하드필터+스코어링+DB 경로만 순수 측정한다. DEST_ADDRESS_RATIO로 일부 요청에
// destAddress를 채워 CommuteFilter 전체 경로(스텁 호출 포함)도 같이 섞을 수 있다 -
// 이땐 .env에 ROUTE_MODE=stub이 돼 있어야 카카오 실API를 안 탄다.
//
//   k6 run -e BASE_URL=http://localhost:8080 -e JWT_SECRET=<.env의 JWT_SECRET> \
//     -e DEST_ADDRESS_RATIO=0.2 loadtest/k6/scenarios/01_write_heavy.js

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, TEST_USER_ID_MIN, TEST_USER_ID_MAX } from '../lib/config.js';
import { mintToken, authHeaders } from '../lib/auth.js';
import { buildConditionRequest, buildConditionRequestWithDest } from '../lib/payloads.js';

const DEST_ADDRESS_RATIO = Number(__ENV.DEST_ADDRESS_RATIO || 0);

export const options = {
  scenarios: {
    write_heavy: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 5 },
        { duration: '1m', target: 5 },
        { duration: '30s', target: 10 },
        { duration: '1m', target: 10 },
        { duration: '30s', target: 20 },
        { duration: '1m', target: 20 },
        { duration: '30s', target: 50 },
        { duration: '1m', target: 50 },
        { duration: '30s', target: 0 },
      ],
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    'http_req_duration{endpoint:create}': ['p(95)<5000'],
    'http_req_duration{endpoint:recompute}': ['p(95)<5000'],
  },
};

function randomUserId() {
  return TEST_USER_ID_MIN + Math.floor(Math.random() * (TEST_USER_ID_MAX - TEST_USER_ID_MIN + 1));
}

export default function () {
  const token = mintToken(randomUserId());
  const auth = authHeaders(token);

  const useDest = Math.random() < DEST_ADDRESS_RATIO;
  const payload = useDest
    ? buildConditionRequestWithDest('서울특별시 중구 세종대로 110')
    : buildConditionRequest();

  const createRes = http.post(
    `${BASE_URL}/api/user-conditions`,
    JSON.stringify(payload),
    Object.assign({}, auth, { tags: { endpoint: 'create' } })
  );

  const createOk = check(createRes, {
    'create: 200': (r) => r.status === 200,
  });

  if (createOk && createRes.json('success')) {
    const conditionId = createRes.json('data.conditionId');

    sleep(0.2);

    const recomputeRes = http.post(
      `${BASE_URL}/api/user-conditions/${conditionId}/recompute`,
      null,
      Object.assign({}, auth, { tags: { endpoint: 'recompute' } })
    );

    check(recomputeRes, { 'recompute: 200': (r) => r.status === 200 });
  }

  sleep(0.5);
}
