// 브레이크포인트 테스트: 쓰기 경로(create+recompute)에 VU를 계속 늘려가면서, 다른
// 시나리오들이 SLA로 삼은 기준(에러율 1% 미만)이나 그보다 빡빡한 레이턴시 기준(p95 3초)을
// 넘는 순간 k6가 스스로 테스트를 중단(abortOnFail)하게 해서 "정확히 몇 VU에서 무너지는가"를
// 찾는다. 01_write_heavy처럼 정해진 stage로 끝내는 대신, 완만하게 계속 올라가는 단일 ramp를
// 쓴다.
//
// 주의: http_req_failed는 테스트 시작부터 누적된 비율이라, 낮은 VU 구간에서 쌓인 성공
// 요청이 나중의 실패를 "희석"할 수 있다 - 그래서 p95 레이턴시 임계값도 같이 걸어서, 에러율이
// 아직 안 튀어도 커넥션 풀/락 대기로 응답이 느려지는 시점을 먼저 잡아낼 수 있게 했다.
//
// 로컬 1대짜리 docker compose 환경이라, 앱/DB 자체의 한계보다 이 머신의 CPU·루프백
// 네트워크·k6 VU 스레드 오버헤드가 먼저 병목이 될 수도 있다는 점은 감안해서 해석할 것.
//
//   k6 run -e BASE_URL=http://localhost:8080 -e JWT_SECRET=<.env의 JWT_SECRET> \
//     loadtest/k6/scenarios/04_breakpoint.js
//
// MAX_VUS/RAMP_DURATION으로 끝까지 안 무너지면 상한/시간을 늘려 재시도.

import http from 'k6/http';
import { check, sleep } from 'k6';
import { BASE_URL, TEST_USER_ID_MIN, TEST_USER_ID_MAX } from '../lib/config.js';
import { mintToken, authHeaders } from '../lib/auth.js';
import { buildConditionRequest, buildConditionRequestWithDest } from '../lib/payloads.js';

const DEST_ADDRESS_RATIO = Number(__ENV.DEST_ADDRESS_RATIO || 0);
const MAX_VUS = Number(__ENV.MAX_VUS || 400);
const RAMP_DURATION = __ENV.RAMP_DURATION || '12m';

export const options = {
  scenarios: {
    breakpoint: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '30s', target: 5 },
        { duration: RAMP_DURATION, target: MAX_VUS },
      ],
      gracefulRampDown: '10s',
    },
  },
  thresholds: {
    http_req_failed: [{ threshold: 'rate<0.01', abortOnFail: true, delayAbortEval: '10s' }],
    http_req_duration: [{ threshold: 'p(95)<3000', abortOnFail: true, delayAbortEval: '10s' }],
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

  const createOk = check(createRes, { 'create: 200': (r) => r.status === 200 });

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
