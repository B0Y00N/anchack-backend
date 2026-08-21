// 스모크 테스트: 부하 없이 VU 1개로 condition/recommendation 전체 흐름이 정상 동작하는지만
// 확인한다. 이게 통과해야 나머지 부하 시나리오 결과를 신뢰할 수 있다.
//
//   k6 run -e BASE_URL=http://localhost:8080 -e JWT_SECRET=<.env의 JWT_SECRET> \
//     loadtest/k6/scenarios/00_smoke.js

import http from 'k6/http';
import { check, fail, sleep } from 'k6';
import { BASE_URL, TEST_USER_ID_MIN } from '../lib/config.js';
import { mintToken, authHeaders } from '../lib/auth.js';
import { buildConditionRequest, buildConditionRequestWithDest } from '../lib/payloads.js';

export const options = {
  vus: 1,
  iterations: 1,
  thresholds: {
    http_req_failed: ['rate==0'],
  },
};

export default function () {
  const token = mintToken(TEST_USER_ID_MIN);
  const auth = authHeaders(token);

  // 1) 생성 (destAddress 없이 - guCodes 기반, 카카오/RouteService 안 탐)
  const createRes = http.post(
    `${BASE_URL}/api/user-conditions`,
    JSON.stringify(buildConditionRequest()),
    auth
  );

  check(createRes, {
    'create: 200': (r) => r.status === 200,
    'create: success=true': (r) => r.json('success') === true,
    'create: conditionId 있음': (r) => r.json('data.conditionId') != null,
  }) || fail(`create 실패: status=${createRes.status}, body=${createRes.body}`);

  const conditionId = createRes.json('data.conditionId');
  console.log(`생성된 conditionId=${conditionId}, 추천 개수=${(createRes.json('data.recommendations') || []).length}`);

  sleep(0.3);

  // 2) 저장
  const saveRes = http.put(`${BASE_URL}/api/user-conditions/${conditionId}/save`, JSON.stringify({ title: 'k6 스모크 테스트' }), auth);
  check(saveRes, { 'save: 204': (r) => r.status === 204 }) || fail(`save 실패: status=${saveRes.status}, body=${saveRes.body}`);

  sleep(0.3);

  // 3) 저장 목록 조회
  const savedRes = http.get(`${BASE_URL}/api/user-conditions/saved`, auth);
  check(savedRes, {
    'saved: 200': (r) => r.status === 200,
    'saved: 방금 만든 조건 포함': (r) => (r.json('data') || []).some((c) => c.conditionId === conditionId),
  }) || fail(`saved 조회 실패: status=${savedRes.status}, body=${savedRes.body}`);

  sleep(0.3);

  // 4) 추천 결과 복원 조회 (재계산 없이 저장된 결과 그대로)
  const recRes = http.get(`${BASE_URL}/api/user-conditions/${conditionId}/recommendations`, auth);
  check(recRes, {
    'recommendations: 200': (r) => r.status === 200,
    'recommendations: 배열': (r) => Array.isArray(r.json('data')),
  }) || fail(`recommendations 조회 실패: status=${recRes.status}, body=${recRes.body}`);

  sleep(0.3);

  // 5) 재계산
  const recomputeRes = http.post(`${BASE_URL}/api/user-conditions/${conditionId}/recompute`, null, auth);
  check(recomputeRes, {
    'recompute: 200': (r) => r.status === 200,
    'recompute: 같은 conditionId': (r) => r.json('data.conditionId') === conditionId,
  }) || fail(`recompute 실패: status=${recomputeRes.status}, body=${recomputeRes.body}`);

  sleep(0.3);

  // 6) destAddress를 채운 생성 - ROUTE_MODE=stub이면 카카오 없이 CommuteFilter 전체 경로를 탐
  const destRes = http.post(
    `${BASE_URL}/api/user-conditions`,
    JSON.stringify(buildConditionRequestWithDest('서울특별시 중구 세종대로 110')),
    auth
  );
  check(destRes, {
    'destAddress create: 200': (r) => r.status === 200,
  }) || fail(`destAddress create 실패: status=${destRes.status}, body=${destRes.body}`);

  sleep(0.3);

  // 7) 저장 취소 (정리)
  const unsaveRes = http.del(`${BASE_URL}/api/user-conditions/${conditionId}/save`, null, auth);
  check(unsaveRes, { 'unsave: 204': (r) => r.status === 204 });
}
