# condition/recommendation 부하테스트 (k6)

condition/recommendation 도메인(`POST /api/user-conditions`, `POST /{id}/recompute`,
`GET /{id}/recommendations`, `GET /saved`, `PUT`/`DELETE /{id}/save`)에 대한 k6 부하테스트.

## 0. 사전 준비

### k6 설치 (아직 없다면)

```bash
winget install k6.k6
```

### 로컬 앱/DB가 이 상태인지 확인

- `docker compose`로 로컬 DB(`mysql` 서비스)를 띄우고 원격 개발 DB를 로컬로 복제해둔 상태
- `.env`에 `ROUTE_MODE=stub` — 카카오 실API 대신 스텁을 타게 함 (destAddress를 채운 요청을 섞을 때 카카오 12.5 req/s 전역 페이싱에 막히지 않기 위함). 코드/설정 변경 후에는 `docker compose up -d --build app`으로 재빌드까지 해야 반영됨
- 테스트 전용 유저 시드 완료: [`seed_k6_test_users.sql`](seed_k6_test_users.sql)을 로컬 DB에 1회 실행해 `user_id` 4~53(`provider_id`='k6-loadtest-001'~'050')이 있어야 함
  ```bash
  docker compose exec -T mysql mysql -uroot -p<DB_ROOT_PASSWORD> anchack < loadtest/seed_k6_test_users.sql
  ```
  정리(부하테스트 다 끝난 뒤): `DELETE FROM users WHERE provider_id LIKE 'k6-loadtest-%';`

### payloads.js의 실제 DB 값 확인

[`k6/lib/payloads.js`](k6/lib/payloads.js) 상단의 `GU_CODES`/`ESSENTIAL_CATEGORIES`/`HOUSE_TYPES`는 로컬 복제 DB 실측값(2026-08-21)으로 박아뒀다. DB를 다시 복제하거나 다른 환경에서 돌릴 때는 아래로 재확인:

```sql
SELECT gu_code, name FROM gus;
SELECT DISTINCT category FROM places;
SELECT DISTINCT house_type FROM property_metrics;
```

특히 `house_type`은 실제 `property_metrics`에 없는 값을 보내면 `HouseTypeBudgetFilter`가 후보를 매번 0개로 걸러버려서 `CommuteFilter`/스코어링까지 못 가고 요청이 "가짜로" 가벼워진다 — 부하테스트 결과가 왜곡되니 주의.

## 1. 시나리오

| 파일 | 목적 | 실행 |
|---|---|---|
| `k6/scenarios/00_smoke.js` | 정합성 확인 (VU 1, 1회) — 본격 부하 전 반드시 먼저 통과시킬 것 | `k6 run -e JWT_SECRET=<값> loadtest/k6/scenarios/00_smoke.js` |
| `k6/scenarios/01_write_heavy.js` | 핵심 쓰기 경로: create+recompute를 VU 0→50까지 램핑. `DEST_ADDRESS_RATIO`로 destAddress 섞는 비율 조절(기본 0) | `k6 run -e JWT_SECRET=<값> -e DEST_ADDRESS_RATIO=0.2 loadtest/k6/scenarios/01_write_heavy.js` |
| `k6/scenarios/02_read_baseline.js` | 읽기 경로 대조군: 저장된 결과 복원(`GET /recommendations`)과 `GET /saved`만 VU 0→100 램핑 | `k6 run -e JWT_SECRET=<값> loadtest/k6/scenarios/02_read_baseline.js` |
| `k6/scenarios/03_mixed.js` | 혼합: 읽기 70% / 생성 20% / 재계산 10% 비율로 VU 0→30 램핑 (실제 트래픽 패턴에 가까운 그림). `READ_RATIO`/`CREATE_RATIO`로 비율 조절 가능 | `k6 run -e JWT_SECRET=<값> loadtest/k6/scenarios/03_mixed.js` |

`JWT_SECRET`은 `.env`의 `JWT_SECRET`과 반드시 동일한 값이어야 한다(HS256 서명 검증). `BASE_URL`은 기본 `http://localhost:8080`.

예시:
```bash
JWT_SECRET=$(grep -oP '^JWT_SECRET=\K.*' .env)
k6 run -e JWT_SECRET="$JWT_SECRET" loadtest/k6/scenarios/00_smoke.js
```

## 2. 다음에 추가할 시나리오

- **브레이크포인트**: `01_write_heavy`의 `stages`를 에러율/레이턴시 임계값 넘을 때까지 계속 올리는 형태로 변형 (`executor: ramping-vus` + 높은 target으로 확장)
- **소크**: `constant-vus` executor로 VU 10~20을 30~60분 유지. 이 경우 `mintToken`이 setup()에서 한 번만 발급되는 `02_read_baseline.js` 패턴을 쓰면 안 됨 — 기본 JWT 만료(1시간)를 넘길 수 있으니 매 iteration마다 새로 발급하거나 `JWT_EXPIRATION_MS`를 늘려서 재빌드해야 함

Prometheus/Grafana 연동은 별도로 진행 예정 (`k6 run --out experimental-prometheus-rw`로 k6 자체 메트릭을 push).
