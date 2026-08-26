# 지표 계산 & 추천 생성 로직

`docs/how_to_calculate_metrics.md`(이하 "기존 문서")를 현재 코드와 대조 검증한 뒤, 지표 계산과
추천(recommendations) 생성 로직 전체를 다시 정리한 문서입니다. 기존 문서는 작성 시점이 뒤섞여
있고 일부 섹션이 최신 코드를 반영하지 못해, 이 문서가 최신 기준의 단일 출처(source of truth)
역할을 하도록 작성했습니다. 문서 맨 끝에 기존 문서와의 차이점을 표로 정리해뒀습니다.

## 전체 그림

```
1. 원본 데이터 적재        places / rental_transactions / (구별 범죄 발생건수·행정동별 평균소음 원본)
                              │
2. 지표 점수 계산(수동 트리거)   POST /api/admin/metrics/recalculate-all
                              │  → culture/food/healthcare/life_convenience/nature/safety/sports/
                              │    transit/silence_metrics, gu_crime_stats에 "시점 스냅샷" 행을
                              │    매번 새로 INSERT (UPDATE 아님, 시계열)
                              ▼
3. 추천 생성                POST /api/user-conditions  또는  POST /api/user-conditions/{id}/recompute
   (compute → persist)        1) 하드필터 → 2) 소프트 스코어링(z-score 가중합) → 3) 상위 5개
                              →  4) reason/caution 생성 → 5) recommendations/recommendation_scores 저장
```

---

## Part 1. 지표 점수 계산 (`metric` 패키지)

### 1.1 공통 수학 유틸 — `ScoreMath`

**패키지 위치가 `metric`이 아니라 `common.util`입니다** (`src/main/java/com/kbait/anchack/common/util/ScoreMath.java`).
`metric` 패키지와 `recommendation` 패키지(소프트 스코어링)가 이 클래스를 공유합니다. private
생성자로 인스턴스화를 막은 순수 정적 유틸입니다.

| 메서드 | 동작 | 예외적 케이스 |
|---|---|---|
| `minMaxNormalize(double[])` | 배열을 0~100으로 정규화 | `max == min`(전부 같은 값)이면 전부 `50.0` |
| `zScore(double[])` | 평균 0, 표준편차 1로 표준화 (모집단 분산, N으로 나눔) | `stddev == 0`이면 전부 `0.0` |
| `log1p(double[])` | `Math.log1p`(자연로그(1+x))를 배열 전체에 적용 | — |
| `clip(value, min, max)` | 범위를 벗어나면 경계값으로 자름 | — |
| `round2(double)` | `BigDecimal`로 변환 후 `HALF_UP`으로 소수 둘째자리 반올림 | — |

### 1.2 `places` 기반 지표 8종 (공통 패턴)

**대상**: `culture`, `food`, `healthcare`, `life_convenience`, `nature`, `sports`, `transit`, `safety`
(safety만 계산 방식이 다름 → 1.3에서 별도 설명)

**원본 카운트 소스**: 전부 `places` 테이블을 `admin_dongs`에 `LEFT JOIN`해서 `category`별로
`COUNT`합니다(예: `CultureMetricMapper.xml` → `category = 'CULTURE'`). `LEFT JOIN`이라 매칭되는
장소가 0개인 동도 결과에 포함됩니다(카운트 0). 카테고리 매핑:

| 지표 | places.category | 카운트 필드 |
|---|---|---|
| culture | `CULTURE` | `culture_count` (1개) |
| nature | `RIVER`, `TRAIL`, `PARK` (3개 합산) | `nature_count` (1개) |
| sports | `SPORTS` | `sports_count` (1개) |
| transit | `SUBWAY_STATION`, `BUS_STOP` | `subway_station_count`, `bus_stop_count` |
| healthcare | `HOSPITAL`, `PHARMACY` | `hospital_count`, `pharmacy_count` |
| food | `RESTAURANT`, `CAFE` | `restaurant_count`, `cafe_count` |
| life_convenience | `MART`, `BANK`, `DEPARTMENT_STORE` | `mart_count`, `bank_count`, `department_store_count` |
| safety | `CCTV`, `STREET_LIGHT`, `POLICE`, `SAFETY_BELL` | 각각 `_count` 컬럼 |

**계산 로직 — `DensityMinMaxScoreCalculator`** (safety 제외 7종에 적용, `metric.service` 패키지,
`@Component`, 순수 계산이라 유닛 테스트로 검증됨):

1. 카운트 항목마다(예: life_convenience면 mart/bank/department_store 각각) 독립적으로:
   - `인구밀도 = count / dong_population × 10,000` (분모 0/null이면 0으로 처리)
   - `면적밀도 = count / dong_area`
   - 두 밀도값을 각각 **전체 426개 행정동 기준**으로 `minMaxNormalize` → 0~100
   - `항목별 점수 = (인구밀도 정규화값 + 면적밀도 정규화값) / 2`
2. 항목이 여러 개면 항목별 점수들을 다시 평균 → 최종 `_score`

카운트를 먼저 합산하지 않고 항목별로 독립 정규화 후 평균 내는 이유: 먼저 합치면 발생 빈도가
큰 항목(예: 마트가 은행보다 훨씬 흔함)이 결과를 지배해버립니다.

**입력 DTO**: `DensityScoreInput`(adminDongId, `counts: Map<String,Long>`, dongPopulation, dongArea)
— `counts`가 Map인 이유는 카운트 항목이 1개인 테이블과 여러 개인 테이블을 같은 구조로 다루기
위함입니다.

### 1.3 안전(safety) 점수 — 전용 계산기

CCTV/가로등/경찰서/안심벨(4개, `places` 기반) + 범죄율(`gu_crime_stats`, 구 단위, 아래 1.4 참고)을
결합하는 별도 로직입니다. `SafetyScoreCalculator`(`metric.service`), 가중치는 클래스 상단
`private static final double` 상수:

```
CRIME_WEIGHT = 0.40   CCTV_WEIGHT = 0.20   STREETLIGHT_WEIGHT = 0.15
POLICE_WEIGHT = 0.15  BELL_WEIGHT = 0.10
```

1. `cctv_per_10k = cctv_count / dong_population × 10,000` (population 0/null이면 0) — 가로등/경찰서/안심벨도 동일
2. `z_cctv = zScore(log1p(cctv_per_10k))` (가로등/경찰서/안심벨도 동일 — 소수 동에 몰린 분포 완충용)
3. `z_crime = zScore(crime_per_10k)` — 범죄율은 이미 비율이라 `log1p` 생략
4. `raw = -0.40×z_crime + 0.20×z_cctv + 0.15×z_streetlight + 0.15×z_police + 0.10×z_bell`
5. `safety_score = clip(50 + 10×raw, 0, 100)`, `round2`

가중치 근거(상식적 우선순위 기반 초기값, 통계적으로 도출된 값 아님 — 추후 실제 사건 데이터나
설문으로 검증/보정 권장):
- **범죄율 40%** — 유일한 실제 결과(outcome) 지표라 가장 신뢰도가 높음
- **CCTV 20%** — 데이터가 가장 촘촘하고 범죄 억제 효과 근거가 많이 축적됨
- **가로등 15%** — 야간 시야 확보는 체감 안전도와 강한 상관(CPTED 이론)
- **경찰서 15%** — 실질 대응력이지만, 지구대/파출소 관할구역이 행정동보다 넓어 많은 동이
  `police_office_count = 0`이 될 수 있는 데이터 한계가 있어 `log1p`로 완충
- **안심벨 10%** — 사후 대응 성격이라 예방 효과가 상대적으로 약함

### 1.4 소음 점수 / 지역구 범죄율 — `places` 기반이 아닌 나머지 2개 지표

CCTV/가로등 같은 나머지 지표와 달리 `places` 카운트가 아니라 별도 원본 데이터(행정동별 평균
소음, 구별 5대 범죄 발생 건수)를 입력으로 씁니다. 계산식 자체는 기존 문서("지역구 범죄율
점수"/"행정동 소음 점수" 섹션)와 동일하고, 나머지 8개와 마찬가지로 재계산할 때마다 새 스냅샷
행을 시계열로 쌓는 방식입니다 — 최신 값은 PK 최댓값(`silence_id`/`gu_crime_id`)으로 선택합니다
(`safety_metrics`가 `gu_crime_stats`를 조인할 때 이미 쓰는 것과 동일한 패턴).

**행정동 소음 점수 (`silence_metrics.silence_score`)**

입력: 행정동별 평균 소음, 행정동별 인구 수

```
silence_score = 정규화(역방향) 평균소음 × 0.7 + 정규화(역방향) 인구 × 0.3   (0~100 스케일)
```

소음·인구가 낮을수록 높은 점수. 원본 소음값 결측(전체 미매칭 26곳 + 매칭됐지만 값 빈칸 5곳,
합 31건)은 `seoul_dong.geojson` 폴리곤 중심좌표(투영좌표계, m 단위) 기준 최근접 5개 행정동의
역거리가중평균(IDW)으로 보간해서 채웁니다.

**지역구 범죄율 (`gu_crime_stats.crime_rate`)**

입력: 행정동별 인구를 합산한 지역구별 인구, 지역구별 5대 범죄 발생 건수

```
crime_rate = 발생건수 / 인구 × 10,000   (인구 1만 명당 범죄 발생 건수, 소수 셋째 자리 반올림)
```

같은 구에 속한 모든 행정동은 이 값을 공유합니다 — `safety_metrics`(1.3)가 조회 시점에
`gu_code`로 조인해서 참조.

두 테이블 모두 나머지 8개 지표와 동일하게 시계열이라 "최신 값"은 PK 최댓값으로 선택합니다:
- `gu_crime_stats`: `MAX(gu_crime_id)` per `gu_code`
- 나머지 9개 지표 테이블(silence 포함)도 전부 동일 패턴 (`MAX(해당_id)` per `admin_dong_id`)

### 1.5 오케스트레이션 서비스 — 10개, 트리거는 관리자 수동 엔드포인트

`Culture/Food/Healthcare/LifeConvenience/Nature/Safety/Sports/Transit/SilenceMetricScoreService`
(9개, 행정동 단위) + 구 단위인 범죄율 서비스(`gu_crime_stats` 갱신) — 전부 동일 패턴의
`void recalculateAll()` 메서드 하나만 가집니다:

1. 매퍼로 원본 데이터 조회 — `places` 기반 8종은 `findAllCountsFromPlaces()`(전체 행정동의
   카운트+인구/면적), 소음은 행정동별 평균소음+인구, 범죄율은 지역구별 발생건수+인구(행정동
   인구를 `gu_code` 기준으로 합산)
2. `Row` → 계산기 입력(`DensityScoreInput`/`SafetyScoreInput`, 소음/범죄율은 각자 전용 입력)으로 변환
3. 계산기 호출 → `Map<adminDongId, score>`(범죄율은 `Map<guCode, crimeRate>`)
4. **새 행을 시계열로 INSERT** (`mapper.insertRows(rows)`) — 기존 행을 UPDATE하지 않습니다.
   매번 실행할 때마다 그 시점 기준 스냅샷 한 행이 쌓입니다.

`@Transactional`이 붙어 있어 도중 실패 시 롤백됩니다.

**트리거**: `MetricAdminController`(`metric.controller`, `POST /api/admin/metrics/recalculate-all`)가
10개 서비스를 순서대로 전부 호출합니다. 클래스 자체가 "개발용 관리 엔드포인트"라고 명시하고
있고, 운영 환경에서 자동으로 도는 스케줄러는 아직 없습니다(`@Scheduled`는 places/전월세
수집 스케줄러에만 있고, 지표 재계산 서비스를 부르는 스케줄러는 없음). 이 컨트롤러가 지금
저장소 전체에서 10개 `recalculateAll()`을 부르는 유일한 호출부입니다(테스트 제외).

`property_metrics`(임대 시세 집계, `_metrics` 계열은 아니지만 관련 있음)는 다른 패턴입니다:
`PropertyMetricAggregationService.recalculate()`가 `rental_transactions`에서 매번
**전체 DELETE 후 새로 집계해서 INSERT**(시계열 아님, 매번 최신 스냅샷 하나만 유지) —
`POST /api/admin/metrics/property/recalculate`로 트리거.

### 1.6 테스트 커버리지

- `ScoreMathTest` — minMax(경계값/동일값), zScore, clip
- `DensityMinMaxScoreCalculatorTest` — 단일/복수 카운트, 0인구/0면적 edge case, 동일값 중립 50
- `SafetyScoreCalculatorTest` — 상대적 순위, 0~100 clip, 단일 후보(분산 0) 중립 50
- `PropertyMetricAggregationServiceTest` — delete→insert 순서, 예외 전파
- 10개 오케스트레이션 서비스·`MetricAdminController`·`log1p`/`round2` 자체는 별도 테스트 없음
- 매퍼(MyBatis SQL) 레벨 테스트도 없음(`metric/mapper` 테스트 디렉터리는 `.gitkeep`만 있음)

---

## Part 2. 추천(recommendations) 생성 로직

### 2.1 진입점 2가지

| 엔드포인트 | 동작 |
|---|---|
| `POST /api/user-conditions` | 새 조건 저장 + 즉시 추천 생성 (`ConditionServiceImpl.createAndRecommend`) |
| `POST /api/user-conditions/{conditionId}/recompute` | 이미 저장된 조건의 파라미터(하위 테이블 포함)를 그대로 다시 읽어 재계산 (`ConditionServiceImpl.recompute`) |

`recompute`는 프론트가 보낸 원본 요청 대신 DB에 이미 한글 ENUM/만원 단위로 저장된 값을 그대로
읽어 `ConditionBundle`을 재구성하므로 코드 변환 단계가 없습니다.

### 2.2 전체 흐름 — `compute`/`persist` 분리 + 데드락 재시도

`RecommendationService`는 메서드가 하나가 아니라 **`compute()`(순수 계산, 트랜잭션 없음)와
`persist()`(DB 반영, 트랜잭션 있음)로 나뉘어 있습니다.**

```
compute(condition)                                     — RecommendationServiceImpl, @Transactional 없음
1. HardFilterService.filter(condition)                    → 조건을 만족하는 admin_dong 후보 목록
2. RecommendationScoreCalculator.calculate(...)            → 후보별 totalScore 산출 + rank 부여
3. 상위 5개(TOP_N) 추출
4. admin_dong 이름/좌표 조회
5. reason/caution 생성 — 후보별 병렬 호출(CompletableFuture, 스레드풀)
→ List<RecommendationRow> 반환 (아직 DB에 안 씀)

persist(conditionId, rows)                             — @Transactional, 데드락 재시도 대상
6. recommendations/recommendation_scores DELETE 후 INSERT (해당 condition_id 기준 전체 교체)
```

나뉜 이유는 두 가지입니다.

1. **DB 커넥션을 오래 붙잡지 않기 위해** — `compute()` 안에서 카카오 통근 API·OpenAI reason
   생성 같은 외부 네트워크 호출이 일어나는데, 여기에 `@Transactional`을 걸면 그 호출이 끝날
   때까지 커넥션을 붙잡아두게 됩니다.
2. **데드락 재시도가 외부 호출을 반복하지 않게 하기 위해** — k6 부하테스트에서 VU 20대부터
   `recommendations` INSERT가 `admin_dongs`를 참조하는 FK 때문에(소수의 상위권 행정동에 여러
   트랜잭션의 참조가 몰림) 동시 요청끼리 MySQL 데드락이 걸려 조건 생성 요청의 4.37%가 실패하는
   걸 확인했습니다(`docs/LOAD_TEST_REPORT.md`). 이걸 고치려고 `DeadlockRetry.execute(...)`
   (`common.util`, 최대 3회, 매 시도마다 대기시간을 늘리고 지터를 섞음)로 `persist()` 호출을
   감싸는데, 만약 `compute()`까지 같은 트랜잭션/재시도 범위에 있었다면 재시도할 때마다 카카오/
   OpenAI 호출까지 중복 실행됐을 것입니다. `compute()`는 재시도 대상에서 제외하고 `persist()`만
   재시도하도록 나눠서 이 문제를 피합니다.

호출부(`ConditionServiceImpl`)는 이렇게 씁니다: `compute()`로 계산한 뒤,
`DeadlockRetry.execute(() -> recommendationService.persist(conditionId, computed))`로 반영.
`ConditionServiceImpl` 자체는 `createAndRecommend`/`recompute` 둘 다 `@Transactional`이 아닙니다
— 붙이면 그 안의 모든 호출이 하나의 트랜잭션/커넥션으로 묶여 위 2번 문제로 되돌아가기 때문입니다.

`persist()` 내부에서 `recommendation_scores`를 `recommendations`보다 먼저 삭제하는 이유: FK에
`ON DELETE CASCADE`가 없어서 순서를 바꾸면 제약 위반이 납니다. 후보가 0건이면 `insertBatch`
자체를 호출하지 않습니다 — MyBatis `<foreach>`가 빈 리스트에 대해 `VALUES` 뒤에 아무것도 없는
SQL을 그대로 렌더링해 문법 오류가 나기 때문입니다(하드필터 쪽도 동일한 이유로 각 단계 후보
0건 시 즉시 반환).

### 2.2.1 조건 저장(`UserConditionWriter`)과 실패 시 보상 삭제

`user_conditions` + 하위 테이블(`condition_weights`/`condition_essentials`/
`preferred_house_types`/`condition_gus`) 저장·삭제는 `ConditionServiceImpl`이 직접 하지 않고
별도 빈 `UserConditionWriter`(`condition.service.impl`)가 맡습니다. `@Transactional`이 붙은
메서드를 같은 클래스 안에서 `this.method()`로 호출하면 프록시를 안 거쳐 트랜잭션이 적용되지
않는 self-invocation 문제 때문에, 저장 로직을 별도 빈으로 뺐습니다.

- `insert(...)` — `@Transactional`. 한글로 이미 번역된 값을 받아 순수하게 저장만 함(영문→한글
  번역은 `ConditionServiceImpl`의 책임).
- `delete(conditionId)` — `@Transactional`. FK 때문에 하위 테이블부터 지우고 `user_conditions`를
  마지막에 지움.

`createAndRecommend()` 흐름: `UserConditionWriter.insert()`로 먼저 커밋 → `compute()` →
`DeadlockRetry.execute(() -> persist(...))`. 저장이 이미 별도 트랜잭션으로 커밋된 뒤라, 계산/반영
단계가 실패하면(런타임 예외) **추천 결과 없는 "고아" 조건**이 남을 수 있습니다. 이걸 막으려고
`compute()`/`persist()` 호출만 `try-catch`로 감싸고, 실패 시 `catch (RuntimeException e)`에서
`UserConditionWriter.delete(conditionId)`로 방금 만든 조건을 보상 삭제(compensating delete)한
뒤 원래 예외를 그대로 다시 던집니다. 삭제 자체가 또 실패해도 원래 예외를 가리지 않도록 별도로
잡아 로그만 남깁니다.

`toResponse(...)`(DB 응답용 DTO로 변환하는 순수 함수)는 **의도적으로 이 `try` 밖에** 있습니다.
`persist()`가 이미 성공해 `recommendations`/`recommendation_scores`가 커밋된 뒤이므로 여기서
예외가 나도 정리 대상이 아닙니다 — 만약 `try` 안에 있었다면, `UserConditionWriter.delete()`가
`recommendations`는 못 지우면서 `user_conditions`만 지우려다 그 FK 때문에 실패하고, 그 실패를
삼키는 사이 하위 테이블(가중치 등)만 사라진 반쯤 망가진 조건이 남는 문제가 있었습니다(PR
리뷰로 발견, 이후 범위를 좁힘).

`recompute()`는 이 보상 삭제가 필요 없습니다 — 새로 저장할 게 없고(기존 `user_conditions` 그대로
사용), `persist()`가 실패하면 그 자체가 `@Transactional`이라 기존 `recommendations`가 그대로
롤백되어 남기 때문에 애초에 고아 상태가 생기지 않습니다. `recompute()`가 성공적으로 `persist()`를
마친 뒤에만 `userConditionMapper.markLatest(conditionId)`가 실행됩니다 — 재시도를 다 써버리고
예외가 전파되면 그 지점에서 `markLatest`는 호출되지 않습니다.

테스트: `DeadlockRetryTest`(재시도 횟수/백오프, 대기 중 인터럽트 처리), `UserConditionWriterTest`
(insert/delete 각각의 저장·삭제 대상 검증), `ConditionServiceImplTest`/`RecommendationServiceImplTest`
(compute/persist 분리 호출 순서, 계산 실패 시 보상 삭제 호출 여부)가 이 영역을 커버합니다.

### 2.3 하드필터 (`HardFilterServiceImpl`) — 비용이 싼 순서로 적용

외부 API(카카오 통근 경로) 호출 대상을 최소화하려고 아래 순서로 필터링하고, **각 단계 후보가
0개가 되면 즉시 빈 리스트를 반환**하고 다음 단계를 호출하지 않습니다(빈 `IN (...)` SQL 방지).

1. **기본 후보 resolve** — `destAddress`가 있으면(통근 조건 검색) `guCodes`를 무시하고 전체
   행정동을 후보로 삼습니다(통근 필터가 어차피 좌표 기반으로 전체를 대상으로 필터링하므로).
   `destAddress`가 없으면 `guCodes`로 필터링(둘 다 없으면 전체).
2. **`EssentialInfraFilter`** — `condition_essentials`(한글, 예: "편의점")를 `places.category`로
   변환한 뒤, 요구 카테고리를 **전부** 보유한 행정동만 통과(부분집합이면 탈락).
3. **`HouseTypeBudgetFilter`** — `preferred_house_types`가 비어있으면 전체 주거유형(오피스텔/빌라/
   단독/다가구/아파트/원룸) 대상, 선택했으면 그 중 **하나라도**(ANY) 예산 조건을 만족하면 통과.
   `property_metrics`에서 (행정동, rentalType, houseType)별 최신 `avg_deposit`/`avg_rent`/`avg_area`를
   조회해 `avgDeposit ≤ maxDeposit`(둘 다 만원 단위) AND `avgRent ≤ maxRent` AND `avgArea ≥ minArea`
   판정. `property_metrics`에 해당 조합 데이터가 아예 없으면 자동 탈락. 관리비(`max_maintenance_fee`)는
   대응 컬럼이 없어 필터에서 제외.
4. **`CommuteFilter`** — `destAddress`가 없으면 그대로 통과(통근 정보 없이). 있으면:
   - 후보가 50건(`MAX_COMMUTE_ROUTE_CANDIDATES`) 초과 시, 목적지와의 **직선거리(Haversine)** 기준
     가까운 순으로 잘라서 그 안에서만 실제 API를 호출(순수 사전 필터링용, 실제 통근시간 판단에는
     안 씀)
   - 고정 스레드풀(`COMMUTE_CALL_CONCURRENCY = 10`)로 병렬 호출, 애플리케이션 전체 호출 간격은
     `KakaoTransitDirectionsClient`가 80ms(`PACING_INTERVAL_MS`)로 pacing(JVM 인스턴스 범위,
     멀티 인스턴스 확장 시 재검토 필요)
   - 카카오 429(TooManyRequests)는 500ms 대기 후 1회 재시도
   - 경로를 못 찾은 후보(`RouteNotFoundException`)는 조용히 제외, 카카오 API 자체 실패
     (`KakaoRouteApiException`)는 조건 불만족이 확인된 게 아니므로 통근 정보 없이 결과에 포함
   - `maxCommuteTime`/`maxTransferCount` 판정은 통근 정보가 없는 후보(API 실패)는 통과시킴
     (소프트 처리)

### 2.4 소프트 스코어링 (`RecommendationScoreCalculator`)

9개 카테고리: `CULTURE, TRANSIT, SPORTS, NATURE, CONVENIENCE, HEALTHCARE, FOOD, SAFETY, SILENCE`
(SILENCE는 Part 1.4의 `silence_metrics` 시계열 점수를 그대로 사용 — 다른 8개와 동일하게 `null`인
행정동만 해당 카테고리 z-score 계산에서 제외됩니다).

1. `MetricScoreMapper.findAllLatestScores()`로 **전체 426개 행정동**의 카테고리별 최신 점수 조회
   (각 지표 테이블에 `MAX(PK)` per `admin_dong_id` 서브쿼리로 조인 — Part 1.5의 시계열 저장
   방식과 짝을 이루는 패턴)
2. 카테고리별로 **전체 행정동 기준** z-score 표준화(검색 조건과 무관하게 고정된 모집단 — 후보
   목록이 아니라 전체 도시 기준). 해당 카테고리 값이 없는(`null`) 행정동은 그 카테고리 z-score
   계산에서 제외됩니다.
3. 사용자가 고른 `priorityCategories`(최대 3개)를 `condition_weights`에 순서대로
   `size, size-1, ..., 1` 가중치로 저장(1번째=3점, 2번째=2점, 3번째=1점) → 합이 1이 되도록 정규화
4. 후보별로, 가중치를 준 카테고리 중 **해당 후보에 값이 있는 카테고리만** 골라
   `weightedScore = weight × zScore` 계산 → 전부 합산해 `rawTotal`
5. `totalScore = clip(50 + 10×rawTotal, 0, 100)` — safety_score와 동일한 T-score 변환
6. `dataCoverageRate = (값이 있던 카테고리 수 / 가중치 준 카테고리 수) × 100`
7. `totalScore` 내림차순으로 `rank` 부여

### 2.5 reason/caution 생성

`RecommendationReasonClient` 인터페이스, `recommendation.reason.mode` 프로퍼티(기본값 `stub`)로
구현체를 전환합니다(`RecommendationConfig`):

- **`stub`(기본)** — `StubRecommendationReasonClient`, 항상 고정 문구 `"추후 openai api 호출"`을
  두 필드에 채움. 토큰 비용 없이 나머지 파이프라인을 테스트할 때 사용.
- **`openai`** — `OpenAiRecommendationReasonClient`, `OpenAiProxyClient`로 실제 호출. 한 번의
  호출로 두 필드를 받기 위해 "추천 이유: .../ 주의사항: ..." 두 줄 형식만 응답하도록 프롬프트로
  강제하고, 그 형식대로 파싱. 프롬프트에는 행정동명·총점·통근시간(있으면)·카테고리별 원점수(가중치
  높은 순)를 포함하고, "위에 나열된 카테고리 외 주제(교통/위치/조명/상권 등) 언급 금지", "구체적
  사실 지어내기 금지", "~해요체 통일" 등을 명시적으로 지시.
  - API 호출 실패(`OpenAiApiException`) 또는 응답 형식이 어긋나 파싱 실패(`IllegalStateException`)
    시 예외를 전파하지 않고 플레이스홀더로 폴백 — 이미 하드필터·소프트스코어링을 통과해 상위
    5개로 선정된 결과 자체를 reason 생성 실패 때문에 날리지 않기 위함(CommuteFilter가 카카오
    API 실패를 흡수하는 것과 같은 원칙)
- 후보별 순차 호출 시 read-timeout이 누적(최악 TOP_N × timeout)되는 걸 피하려고 고정
  스레드풀로 병렬 호출

### 2.6 저장되는 것 / 저장되지 않는 것

`recommendations` 테이블 컬럼(V8 기준): `recommendation_id, condition_id, admin_dong_id,
total_score, data_coverage_rate, commute_time, transfer_count, route, transport_type, line_num,
vehicle_type, walk_min, transit_min, rank, recommendation_reason, caution, created_at, updated_at`.

- `route`/`transport_type`/`line_num`/`vehicle_type`/`walk_min`/`transit_min`(통근 상세)은
  **V8부터 저장됩니다.** 그 이전엔 생성 시점 HTTP 응답에만 존재하고 DB에는 없어서, 저장된
  조건을 나중에 다시 조회(`GET /{conditionId}/recommendations`)하면 이 필드들이 항상 null이었음.
  지금은 생성/재계산 시점에 함께 저장되므로 그대로 복원됩니다.
- `guName`/`dongName`/`latitude`/`longitude`, `categoryBreakdowns`는 **저장되지 않습니다**
  (`RecommendationRow`의 응답 조립 전용 필드). 저장된 조건을 재조회할 때는 `admin_dong_id`로
  `admin_dongs`를 다시 조회해서 채웁니다(`ConditionServiceImpl.getRecommendations`).
- `recommendation_scores`(카테고리별 breakdown)는 별도 테이블에 저장되지만, 저장된 조건
  재조회 API는 이 테이블을 다시 읽지 않습니다(현재는 생성 직후 응답에만 활용).

### 2.7 조건 저장/재계산/조회 (`condition` 도메인)

`user_conditions`에 `is_saved`(기본 `FALSE`), `is_latest`(기본 `TRUE`), `title` 컬럼이 있습니다.

| 엔드포인트 | 동작 |
|---|---|
| `PUT /api/user-conditions/{id}/save` | `is_saved = TRUE`. body의 `title`이 있으면(공백만이면 무시) 함께 갱신, 없으면 기존 title 유지. 멱등. |
| `DELETE /api/user-conditions/{id}/save` | `is_saved = FALSE`. 멱등. |
| `GET /api/user-conditions/saved` | 로그인 사용자의 `is_saved=TRUE`인 조건 목록, 최신순. |
| `GET /api/user-conditions/{id}/recommendations` | **재계산 없이** 저장된 `recommendations`를 rank순 그대로 반환. |
| `POST /api/user-conditions/{id}/recompute` | 하위 테이블을 그대로 읽어 재계산(2.1 참고), 성공 시 `is_latest = TRUE`로 갱신. |

`is_latest`를 `FALSE`로 되돌리는 로직(예: 관련 지표가 재계산돼 결과가 오래됐음을 자동 감지)은
아직 없습니다 — 현재는 `recompute` 성공 시 `TRUE`로 세팅하는 것만 구현돼 있고(2.2.1 참고),
언제 `FALSE`가 돼야 하는지 판단하는 부분은 추후 작업입니다.

모든 조회/저장/재계산 엔드포인트는 요청한 `userId`가 조건의 소유자가 아니면 403
(`ForbiddenException`), 조건 자체가 없으면 404(`NotFoundException`)를 반환합니다.

---

## 부록: `how_to_calculate_metrics.md` 대비 변경/정정 사항

| 기존 문서 주장 | 실제 코드 | 비고 |
|---|---|---|
| `ScoreMath`가 `metric` 패키지에 있는 것처럼 서술 | `common.util.ScoreMath` | `recommendation` 패키지와 공유 |
| 매퍼 메서드명 `findAllForScoreCalculation()` | 실제로는 `findAllCountsFromPlaces()` | 8개 매퍼 전부 동일 |
| 저장 방식이 `mapper::updateScore`(UPDATE) | 실제로는 `insertRows()`(매번 새 행 INSERT, 시계열) | "최신 값 = PK MAX" 조회 패턴과 일치 |
| "지금은 이 메서드를 부르는 컨트롤러나 스케줄러가 없다" | `MetricAdminController`(수동 관리자 엔드포인트)가 이미 존재 | 스케줄러는 여전히 없음(그 부분은 여전히 맞음) |
| "추후 사용자 가중치 기반 추천 계산... 아직 미착수" | `RecommendationScoreCalculator`로 이미 구현 완료 | Part 2.4 참고 |
| `DensityScoreInput`/`SafetyScoreInput` 위주로만 서술 | 실제로는 매퍼가 먼저 `*MetricRow` DTO로 결과를 받고 `toInput()`으로 변환 | 7종 각각 `CultureMetricRow` 등 존재 |
| 소음 점수 계산식(정규화×0.7+정규화×0.3, IDW 보간)을 코드처럼 서술 | 해당 계산을 수행하는 코드가 저장소에 전혀 없음 | 외부/수동 프로세스로 추정(Part 1.4) |
| 안전 점수 섹션이 문서 안에서 두 번 거의 동일하게 반복 | — | 이 문서에서는 한 번만 정리 |

기존 문서의 안전/소음 계산 "설계 의도"(왜 그런 가중치·변환을 택했는지) 서술은 여전히 유효한
배경 설명이라 이 문서에도 요약해 반영했습니다. 원본 문서는 그대로 두었으니, 세부 수치 근거의
1차 자료가 필요하면 참고하세요(단, "코드 설명"/"트리거 없음"/"미착수" 부분은 위 표 기준으로
최신 상태가 아님을 감안해서 읽어야 합니다).
