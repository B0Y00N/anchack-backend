# 지표별 점수 계산 로직

## 지역구 범죄율 점수

### 입력값

- 행정동별 인구를 이용한 지역구별 인구
- 지역구별 5대 범죄 발생 건수(2024 기준)

### 출력값

- 두 값을 이용한 1만 명 당 범죄 발생 건수를 점수로 활용
- `crime_rate` = 발생건수 / 인구 × 10,000 (소수 셋째 자리 반올림)
- `data_date` = '2024-12-31' 고정

## 행정동 소음 점수

### 입력값

- 행정동별 평균 소음
- 행정동별 인구 수

### 출력값

- `silence_score = 정규화(역방향) 평균소음 × 0.7 + 정규화(역방향) 인구 × 0.3` (0~100 스케일, 소음·인구가 낮을수록 높은 점수)
- 결측 소음값 31건(전체 미매칭 26곳 + 매칭됐지만 값 빈칸 5곳)은 `seoul_dong.geojson` 폴리곤 중심좌표(투영좌표계, m 단위) 기준 최근접 5개 행정동의 역거리가중평균(IDW)으로 보간
- `data_date`는 CSV의 기준일자(`2025-06-30`) 사용, `silence_id`/`admin_dong_id`는 1~426 모두 유일하게 매핑됨을 검증 완료
- `created_at`/`updated_at`은 DDL의 `DEFAULT CURRENT_TIMESTAMP`에 위임(컬럼 생략)

## 행정동 치안 점수(보완하거나 보완 방향 생각해볼 것)

### 설계 원칙

1. **스케일이 다른 지표를 그대로 더하면 안 됩니다.** CCTV는 동당 3~436대, 범죄건수는 인구 1만명당 비율(보통 한 자리~두 자리 수)로 단위 자체가 다릅니다. 반드시 정규화(표준화) 후 결합해야 합니다.
2. **범죄율은 위험(-) 요인, 나머지 4개는 보호(+) 요인**입니다. 부호를 반대로 결합해야 "안전할수록 점수가 높다"는 의미가 성립합니다.
3. **분포가 한쪽으로 크게 치우쳐 있습니다(skewed).** CCTV·안심벨 카운트는 소수 동에 몰려 있어(예: 3 vs 436) 그대로 표준화하면 이상치 몇 개가 전체를 왜곡합니다. `log(1+x)` 변환 후 표준화하는 걸 권장합니다.

### **단계별 계산 로직**

**1단계 — 노출 기준 정규화 (동별 규모 차이 보정)**

`gu_crime_stats`가 "인구 1만명당" 기준이므로, 나머지 지표도 같은 기준(인구)으로 맞추는 걸 권장합니다. 단, CCTV/가로등/안심벨은 "공간을 지키는" 시설이라 면적 기준도 의미가 있어서 — 둘 다 참고하되 주 지표는 인구 기준으로 통일하는 게 결합 시 해석이 쉽습니다.

```
cctv_per_10k   = cctv_count / (dong_population / 10000)
light_per_10k  = street_light_count / (dong_population / 10000)
bell_per_10k   = safety_bell_count / (dong_population / 10000)
police_per_10k = police_office_count / (dong_population / 10000)
crime_per_10k  = gu_crime_stats 값 그대로 (해당 gu의 모든 동에 동일 값 부여)
```

⚠️ 경찰서는 특이 케이스입니다. 지구대/파출소는 관할구역이 행정동보다 넓어서, 실제로 많은 동이 `police_office_count = 0`이 될 겁니다(관할서가 옆 동에 있을 뿐 부재가 아님). 이건 정규화로 해결이 안 되는 데이터 한계라, 가중치를 낮게 주거나 `log(1+count)`로 완충하는 걸 권장합니다.

**2단계 — 로그 변환 후 Z-score 표준화 (426개 동 전체 기준)**

```
z_cctv   = zscore(log(1 + cctv_per_10k))
z_light  = zscore(log(1 + light_per_10k))
z_bell   = zscore(log(1 + bell_per_10k))
z_police = zscore(log(1 + police_per_10k))
z_crime  = zscore(crime_per_10k)          # 범죄는 이미 비율이라 로그 생략 가능
```

**3단계 — 가중합**

```
raw_score = (0.40 * -z_crime)
          + (0.20 * z_cctv)
          + (0.15 * z_light)
          + (0.15 * z_police)
          + (0.10 * z_bell)
```

가중치 근거(조정 가능한 출발점입니다):

- **범죄율 40%** — 유일한 실제 결과(outcome) 지표라 가장 신뢰도가 높음. 나머지는 전부 "예방 자원"이라는 간접 지표라 outcome보다 낮게.
- **CCTV 20%** — 데이터가 가장 촘촘하고(59,737개 지점), 범죄 억제·수사 효과에 대한 근거가 가장 많이 축적된 지표.
- **가로등 15%** — 야간 시야 확보는 체감 안전도와 강한 상관(CPTED 이론의 자연적 감시 요소).
- **경찰서 15%** — 실질 대응력이지만 위에서 언급한 관할구역 불일치 문제로 신뢰도를 약간 낮춤.
- **안심벨 10%** — 사후 대응(사고 발생 후 호출) 성격이라 예방 효과는 상대적으로 약함, 설치 장소도 화장실 등 특정 용도에 편중되는 경향.

**4단계 — 0~100 스케일로 변환**

```
safety_score = 50 + 10 * raw_score   # 대략 평균 50, 표준편차 10 근방으로 분포
safety_score = clip(safety_score, 0, 100)
```

(DECIMAL(6,2)이니 소수 둘째자리까지 반올림)

### **한계로 짚어둘 점**

- `gu_crime_stats`가 구 단위라, **같은 구 안의 모든 동은 범죄율 항목이 동일**합니다. 동 간 점수 차이는 결국 CCTV/가로등/경찰서/안심벨 밀도에서만 벌어진다는 걸 인지하고 계산해야 합니다.
- 가중치(40/20/15/15/10)는 통계적으로 도출한 게 아니라 상식적 우선순위 기반 초기값입니다. 나중에 실제 사건 데이터나 설문 등으로 검증/보정하는 걸 권장합니다.

## 공통 계산 로직 (소음·치안 제외 공통 적용)

### 지표 점수 계산 로직

**1. 원본 카운트**

- 각 지표 테이블의 `_count` 컬럼 = 해당 `admin_dong_id`에 속한 원본 데이터 개수 (예: `culture_count` = CULTURE 카테고리 `places` 개수)

**2. 밀도 계산 (2가지)**

- 인구 대비: `count / dong_population * 10000` (인구 1만 명당 개수)
- 면적 대비: `count / dong_area` (㎢당 개수)

**3. 정규화**

- 두 밀도 값을 각각 전체 426개 행정동 기준으로 min-max 정규화 → 0~100
- 두 정규화 값의 평균을 `_score`로 저장 (예: `culture_score`)

**4. 저장 vs 추천용 분리**

- DB에는 위 0~100 min-max 점수를 그대로 저장 (사용자에게 노출하기 직관적)
- 추후 사용자 가중치 기반 추천 계산 시에는, 저장된 min-max 점수를 다시 z-score(평균 0, 표준편차 1)로 표준화한 뒤 가중합 → 지표 간 분산 차이 때문에 특정 지표가 가중치 의도와 무관하게 결과를 지배하는 걸 방지

### 적용 범위

- 문화, 교통 접근성, 생활 편의 등 → 위 로직 적용
- 소음 취약성, 치안 → 별도 로직

## 코드 설명

`metric/service` 패키지의 커밋 안 된 파일 13개를 역할별로 묶어서 설명드립니다.

## 1. 통계 유틸리티

### `ScoreMath.java`

계산 전반에서 재사용하는 순수 수학 함수 모음입니다. 클래스 자체를 인스턴스화 못 하게 생성자를 private으로 막아뒀습니다(유틸 클래스 관례).

- `minMaxNormalize(double[])` — 배열을 0~100 스케일로 변환. 전체 값이 똑같으면(범위가 0) 극단값 대신 중립값 50을 줍니다.
- `zScore(double[])` — 평균 0, 표준편차 1로 표준화. 표준편차가 0이면 중립값 0.
- `log1p(double[])` — `Math.log1p`(자연로그(1+x))를 배열 전체에 적용. 치안 지표처럼 분포가 한쪽에 쏠린 값의 이상치를 완충할 때 씀.
- `clip(value, min, max)` — 범위를 벗어나면 경계값으로 자름.
- `round2(double)` — `BigDecimal`로 변환 후 소수 둘째자리 반올림(`HALF_UP`). DB 컬럼이 `DECIMAL(6,2)`라 여기서 맞춰줌.

## 2. 계산 입력 DTO

### `DensityScoreInput.java`

소음·치안을 제외한 공통 로직(문화/교통/의료/음식/생활편의/체육/자연)의 입력 단위. `Lombok`으로 getter/setter/생성자 생성. 필드는 `adminDongId`, `counts`(카운트 종류명 → 개수, `Map<String,Long>`), `dongPopulation`, `dongArea`. `counts`가 왜 Map이냐면, 카운트 컬럼이 1개인 테이블(culture_count)과 여러 개인 테이블(mart/bank/department_store)을 같은 구조로 다루기 위해서입니다.

### `SafetyScoreInput.java`

치안 전용 입력. `adminDongId`, `dongPopulation`, `cctvCount`/`streetLightCount`/`policeOfficeCount`/`safetyBellCount`, 그리고 `crimeRatePer10k`(같은 구에 속한 행정동은 다 같은 값). 치안은 항목마다 가중치가 다르고 계산식도 달라서(log1p 적용 여부 등) 공통 DTO를 안 쓰고 따로 뺐습니다.

## 3. 계산기(실제 수식이 들어있는 곳)

### `DensityMinMaxScoreCalculator.java`

공통 로직 담당. `counts`의 각 항목마다 독립적으로 (인구밀도+면적밀도를 각각 min-max 정규화 후 평균) → 항목별 점수를 낸 다음, 항목이 여러 개면 그 점수들을 다시 평균내서 최종 점수. 카운트를 먼저 합치지 않고 항목별로 따로 정규화하는 이유는 지난번에 논의했듯 "빈도 큰 항목이 지배하는 문제" 때문입니다. `@Component`라 스프링 빈으로 등록되고, DB에 안 닿는 순수 계산이라 유닛 테스트로 검증돼 있습니다.

### `SafetyScoreCalculator.java`

치안 전용 계산기. CCTV/가로등/경찰서/안심벨은 인구 1만명당 정규화 → log1p → z-score, 범죄율은 이미 비율이라 log1p 없이 바로 z-score. 그 다음 `crime 40%(음의 방향) + cctv 20% + streetlight 15% + police 15% + bell 10%` 가중합을 내고, `50 + 10*raw`를 0~100으로 clip합니다. 가중치는 전부 클래스 상단 `private static final double` 상수로 빼놨습니다.

## 4. 오케스트레이션 서비스 8개 (전부 똑같은 패턴)

`CultureMetricScoreService`, `FoodMetricScoreService`, `HealthcareMetricScoreService`, `LifeConvenienceMetricScoreService`, `NatureMetricScoreService`, `SafetyMetricScoreService`, `SportsMetricScoreService`, `TransitMetricScoreService` — 이 8개는 전부 구조가 동일합니다:

1. 해당 매퍼를 통해 전체 행정동의 원본 카운트 + 인구/면적을 조회 (`findAllForScoreCalculation()`)
2. 매퍼가 반환한 `Row`(예: `CultureMetricRow`)를 계산기가 이해하는 입력(`DensityScoreInput` 또는 `SafetyScoreInput`)으로 변환 (`toInput()`)
3. 계산기 호출해서 `Map<adminDongId, score>` 결과를 받음
4. `scores.forEach(mapper::updateScore)`로 각 행정동에 대해 UPDATE

`@Transactional`이 붙어 있어서 도중에 실패하면 롤백됩니다. 배치/스케줄러가 원본 카운트를 다 적재한 **뒤에** `recalculateAll()`을 호출하는 게 전제입니다 — 지금은 이 메서드를 부르는 컨트롤러나 스케줄러가 없어서, 실제로 트리거하려면 나중에 그 부분을 만들어야 합니다.

각 서비스가 `toInput()`에서 `counts` 맵에 뭘 넣는지만 다릅니다:

| 서비스 | counts에 들어가는 항목 |
| --- | --- |
| Culture | `culture` 1개 |
| Nature | `nature` 1개 |
| Sports | `sports` 1개 |
| Transit | `subwayStation`, `busStop` |
| Healthcare | `hospital`, `pharmacy` |
| Food | `restaurant`, `cafe` |
| LifeConvenience | `mart`, `bank`, `departmentStore` |
| Safety | (전용 계산기라 counts 안 씀 — `SafetyScoreInput` 필드 그대로 전달) |

## 계산 로직 설명(소음, 범죄율 제외)

**[행정동 치안 점수]**

**설계 원칙**

1. 스케일이 다른 지표를 그대로 더하면 안 됩니다. CCTV는 동당 3~436대, 범죄건수는 인구 1만명당 비율(보통 한 자리~두 자리 수)로 단위 자체가 다릅니다. 반드시 정규화(표준화) 후 결합해야 합니다.
2. 범죄율은 위험(-) 요인, 나머지 4개는 보호(+) 요인입니다. 부호를 반대로 결합해야 "안전할수록 점수가 높다"는 의미가 성립합니다.
3. 분포가 한쪽으로 크게 치우쳐 있습니다(skewed). CCTV·안심벨 카운트는 소수 동에 몰려 있어(예: 3 vs 436) 그대로 표준화하면 이상치 몇 개가 전체를 왜곡합니다. `log(1+x)` 변환 후 표준화하는 걸 권장합니다.

**단계별 계산 로직**

**1단계 — 노출 기준 정규화 (동별 규모 차이 보정)**

`gu_crime_stats`가 "인구 1만명당" 기준이므로, 나머지 지표도 같은 기준(인구)으로 맞추는 걸 권장합니다. 단, CCTV/가로등/안심벨은 "공간을 지키는" 시설이라 면적 기준도 의미가 있어서 — 둘 다 참고하되 주 지표는 인구 기준으로 통일하는 게 결합 시 해석이 쉽습니다.

```
cctv_per_10k   = cctv_count / (dong_population / 10000)
light_per_10k  = street_light_count / (dong_population / 10000)
bell_per_10k   = safety_bell_count / (dong_population / 10000)
police_per_10k = police_office_count / (dong_population / 10000)
crime_per_10k  = gu_crime_stats 값 그대로 (해당 gu의 모든 동에 동일 값 부여)
```

⚠️ `gu_crime_stats`는 구 단위로 시계열이 계속 쌓이는 테이블입니다(`data_date` 컬럼은 이미 제거됨 — 데이터 기준일은 `data_sources`에서 별도 관리). 그래서 "해당 구의 최신 crime_rate"는 **PK(`gu_crime_id`)의 `MAX`값**을 기준으로 선택합니다. `created_at`(초 단위)으로 고르면 배치 삽입 시 같은 초에 여러 행이 들어올 경우 동시에 여러 행과 매칭되어 결과가 뻥튀기될 수 있어, PK인 `gu_crime_id`가 더 안전합니다.

⚠️ 경찰서는 특이 케이스입니다. 지구대/파출소는 관할구역이 행정동보다 넓어서, 실제로 많은 동이 `police_office_count = 0`이 될 겁니다(관할서가 옆 동에 있을 뿐 부재가 아님). 이건 정규화로 해결이 안 되는 데이터 한계라, `log(1+count)`로 완충합니다(2단계에서 다른 지표와 동일하게 log1p 적용, 가중치는 그대로 15% 유지).

**2단계 — 로그 변환 후 Z-score 표준화 (426개 동 전체 기준)**

```
z_cctv   = zscore(log(1 + cctv_per_10k))
z_light  = zscore(log(1 + light_per_10k))
z_bell   = zscore(log(1 + bell_per_10k))
z_police = zscore(log(1 + police_per_10k))
z_crime  = zscore(crime_per_10k)          # 범죄는 이미 비율이라 로그 생략 가능
```

**3단계 — 가중합**

```
raw_score = (0.40 * -z_crime)
          + (0.20 * z_cctv)
          + (0.15 * z_light)
          + (0.15 * z_police)
          + (0.10 * z_bell)
```

가중치 근거(조정 가능한 출발점입니다):

- 범죄율 40% — 유일한 실제 결과(outcome) 지표라 가장 신뢰도가 높음. 나머지는 전부 "예방 자원"이라는 간접 지표라 outcome보다 낮게.
- CCTV 20% — 데이터가 가장 촘촘하고(59,737개 지점), 범죄 억제·수사 효과에 대한 근거가 가장 많이 축적된 지표.
- 가로등 15% — 야간 시야 확보는 체감 안전도와 강한 상관(CPTED 이론의 자연적 감시 요소).
- 경찰서 15% — 실질 대응력이지만 위에서 언급한 관할구역 불일치 문제로 신뢰도를 약간 낮춤.
- 안심벨 10% — 사후 대응(사고 발생 후 호출) 성격이라 예방 효과는 상대적으로 약함, 설치 장소도 화장실 등 특정 용도에 편중되는 경향.

**4단계 — 0~100 스케일로 변환**

```
safety_score = 50 + 10 * raw_score   # 대략 평균 50, 표준편차 10 근방으로 분포
safety_score = clip(safety_score, 0, 100)
```

(DECIMAL(6,2)이니 소수 둘째자리까지 반올림)

**한계로 짚어둘 점**

- `gu_crime_stats`가 구 단위라, 같은 구 안의 모든 동은 범죄율 항목이 동일합니다. 동 간 점수 차이는 결국 CCTV/가로등/경찰서/안심벨 밀도에서만 벌어진다는 걸 인지하고 계산해야 합니다.
- 가중치(40/20/15/15/10)는 통계적으로 도출한 게 아니라 상식적 우선순위 기반 초기값입니다. 나중에 실제 사건 데이터나 설문 등으로 검증/보정하는 걸 권장합니다.

---

**[공통 계산 로직 (소음·치안 제외 공통 적용)]**

**지표 점수 계산 로직**

**1. 원본 카운트**

- 각 지표 테이블의 `_count` 컬럼 = 해당 `admin_dong_id`에 속한 원본 데이터 개수 (예: `culture_count` = CULTURE 카테고리 `places` 개수)
- 테이블에 따라 카운트 항목이 1개인 경우와 여러 개인 경우가 있습니다.
    - 1개: culture(문화), sports(체육), nature(하천/산책로/공원을 합산한 단일 카운트)
    - 여러 개: transit(지하철역+버스정류장), healthcare(병원+약국), food(음식점+카페), life_convenience(마트+은행+백화점)

**2. 밀도 계산 (2가지)**

- 인구 대비: `count / dong_population * 10000` (인구 1만 명당 개수)
- 면적 대비: `count / dong_area` (㎢당 개수)

**3. 정규화**

- 두 밀도 값을 각각 전체 426개 행정동 기준으로 min-max 정규화 → 0~100

**4. `_score` 산출 — 카운트 항목 개수에 따라 다르게 처리**

- **카운트 항목이 1개인 경우**: 두 정규화 값(인구 기준·면적 기준)의 평균을 그대로 `_score`로 저장
- **카운트 항목이 여러 개인 경우**: 항목마다 "두 정규화 값의 평균"을 먼저 구하고(= 항목별 점수), 그 항목별 점수들을 다시 평균 내어 `_score`로 저장
    - 예) life_convenience: mart 점수(인구·면적 정규화 평균), bank 점수, department_store 점수를 각각 구한 뒤 세 값을 평균
    - 이렇게 항목별로 먼저 독립 정규화하는 이유: 카운트를 먼저 합산한 뒤 정규화하면 원래 발생 빈도가 큰 항목(예: 마트가 은행보다 훨씬 흔함)이 결과를 지배해버립니다. 항목별로 따로 정규화한 뒤 평균해야 각 항목이 동등한 비중으로 반영됩니다.

**5. 저장 vs 추천용 분리**

- DB에는 위 0~100 min-max 점수를 그대로 저장 (사용자에게 노출하기 직관적)
- 추후 사용자 가중치 기반 추천 계산 시에는, 저장된 min-max 점수를 다시 z-score(평균 0, 표준편차 1)로 표준화한 뒤 가중합 → 지표 간 분산 차이 때문에 특정 지표가 가중치 의도와 무관하게 결과를 지배하는 걸 방지 (recommendation 도메인에서 별도 구현 예정, 아직 미착수)

**적용 범위**

- 문화(culture), 교통 접근성(transit), 체육(sports), 자연(nature), 생활 편의(life_convenience), 의료(healthcare), 식음료(food) → 위 로직 적용
- 소음 취약성(silence), 치안(safety) → 별도 로직