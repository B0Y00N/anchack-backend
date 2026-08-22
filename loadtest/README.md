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
| `k6/scenarios/04_breakpoint.js` | 브레이크포인트: create+recompute에 VU를 0→`MAX_VUS`(기본 400)까지 완만하게 계속 올리다가 에러율 1% 또는 p95 3초를 넘으면 자동 중단(`abortOnFail`) — 정확히 몇 VU에서 무너지는지 찾는 용도 | `k6 run -e JWT_SECRET=<값> loadtest/k6/scenarios/04_breakpoint.js` |
| `k6/scenarios/05_soak.js` | 소크: 03_mixed와 동일한 읽기70/생성20/재계산10 비율을 `constant-vus`(기본 15)로 30~60분(기본 `DURATION=30m`) 유지 — 메모리/커넥션 누수, 장시간 성능 저하 확인용. iteration마다 토큰을 새로 발급해 장시간 실행 중 JWT 만료를 피함 | `k6 run -e JWT_SECRET=<값> -e VUS=15 -e DURATION=30m loadtest/k6/scenarios/05_soak.js` |

`JWT_SECRET`은 `.env`의 `JWT_SECRET`과 반드시 동일한 값이어야 한다(HS256 서명 검증). `BASE_URL`은 기본 `http://localhost:8080`.

예시:
```bash
JWT_SECRET=$(grep -oP '^JWT_SECRET=\K.*' .env)
k6 run -e JWT_SECRET="$JWT_SECRET" loadtest/k6/scenarios/00_smoke.js
```

## 2. Prometheus + Grafana

기본 `docker compose up`에는 안 뜬다 - 모니터링 스택은 `monitoring` profile로 분리해뒀다.

```bash
docker compose --profile monitoring up -d mysqld-exporter prometheus grafana
```

- **Prometheus**: http://localhost:9090 — `loadtest/monitoring/prometheus.yml`이 `mysqld-exporter`(MySQL 지표)를 스크랩하고, `--web.enable-remote-write-receiver`로 k6가 직접 push하는 것도 받는다.
- **Grafana**: http://localhost:3000 — 로컬 전용이라 익명 admin 접속 허용해둠(로그인 없이 바로 들어가짐). Prometheus 데이터소스는 `loadtest/monitoring/grafana-datasources.yml`로 자동 등록됨. 처음 들어가면 Dashboards → New → Import에서 아래 두 개를 데이터소스는 Prometheus로 선택해서 넣으면 됨:
  - **19665** — k6 공식 Prometheus 대시보드 (요청 처리량/레이턴시/에러율)
  - **7362** — MySQL Overview (mysqld_exporter용 — 커넥션 수, 슬로우 쿼리, InnoDB 상태)

k6 시나리오를 돌릴 때 `--out experimental-prometheus-rw`만 추가하면 Prometheus로 메트릭이 push된다:

```bash
k6 run -e BASE_URL=http://localhost:8080 -e JWT_SECRET="$JWT_SECRET" \
  --out experimental-prometheus-rw \
  loadtest/k6/scenarios/01_write_heavy.js
```

기본 push 대상은 `http://localhost:9090/api/v1/write`라 별도 설정 없이 위 명령으로 바로 동작한다. 다른 주소를 쓰려면 `K6_PROMETHEUS_RW_SERVER_URL` 환경변수로 덮어쓸 것. p95/p99까지 대시보드에서 보고 싶으면 `K6_PROMETHEUS_RW_TREND_STATS="p(95),p(99)"`도 같이 넘기면 된다.

**주의**: 이 프로젝트는 Spring Boot가 아니라 Spring MVC라 Actuator/Micrometer가 없다 - 지금 구성은 **k6 자체 메트릭 + MySQL 서버 메트릭**만 본다. JVM/Tomcat(힙, GC, 커넥션 풀 대기 큐 등) 시각화는 아직 없음 - 필요해지면 Prometheus JMX Exporter를 javaagent로 붙이는 걸 추후 검토.
