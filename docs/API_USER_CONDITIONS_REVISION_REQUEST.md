# 추천 API 확장 요청 (프론트 → 백엔드)

`API_USER_CONDITIONS.md` 기준으로 `POST /api/user-conditions` 연동을 마쳤습니다. 다만 현재 응답이
`adminDongId`(숫자)만 내려줘서, 프론트가 이미 갖고 있던 화면(추천 리스트 카드 / 상세보기 / 비교)을
채우려면 문서의 "미확정" 항목으로 남겨둔 표시용 정보가 필요합니다. 실제 화면 코드 기준으로 어떤
필드가 어디에 쓰이는지 정리했습니다.

우선순위는 두 단계로 나눴습니다.

- **P0** — 지금 배포된 추천 리스트 화면이 "행정동 #12" 같은 placeholder 대신 실제 동 이름을
  보여주기 위해 꼭 필요합니다. → 반영 완료. 프론트도 `guName`/`dongName`을 카드에 적용했습니다.
- **P1** — 리스트에서 "상세 보기"/"비교 담기"를 눌렀을 때 뜨는 상세·비교 화면용입니다. 현재는
  이 데이터가 없어서 클릭 시 "곧 제공될 예정이에요" 토스트만 띄우고 있습니다.
- **P2** — 상세 화면 탭(통근/치안/생활 인프라) 안의 지도에 찍는 장소 마커용입니다. `places` 테이블
  기반 신규 요청입니다.
- **P3** — 로딩 화면 UX 개선용입니다. 필수는 아니고, 있으면 로딩 중 "몇 개 동에서 몇 개까지
  좁혔는지"를 실제 숫자로 보여줄 수 있습니다.

## P0. `POST /api/user-conditions` 응답에 표시용 필드 추가 — ✅ 완료

`recommendations[]` 각 항목에 아래 필드를 추가해주세요.

| 필드 | 타입 | 설명 | 사용처 |
|---|---|---|---|
| `guName` | string | 구 이름 (예: "은평구") | 카드 제목 |
| `dongName` | string | 행정동 이름 (예: "증산동") | 카드 제목 |
| `lat` / `lng` | number \| null | 좌표 | 결과 지도에 추천 동네 핀 표시 (현재 지도는 구 폴리곤만 표시 중이라 당장 급하진 않지만, 같이 주시면 좋습니다) |

기존 `adminDongId`는 그대로 두고 위 필드만 추가하는 additive 변경입니다. 요청 바디는 변경 없습니다.

### 응답 예시 (추가된 필드만 표시)

```json
{
    "adminDongId": 1,
    "guName": "은평구",
    "dongName": "증산동",
    "lat": 37.5871,
    "lng": 126.9095,
    "totalScore": 60.00,
    ...
}
```

## P1. 행정동 상세 정보 조회 API (신규)

문서 하단 "미확정/추후 반영 사항"에 이미 예고된 항목입니다. 상세보기·비교 화면은 아래처럼
"조건에 따라 달라지는 값"과 "동네 자체의 고정 정보"가 섞여 있어서, 두 부분을 나눠 요청드립니다.

### P1-a. 통근 관련 — `POST /api/user-conditions` 응답에 같이 추가

목적지(destAddress)에 따라 달라지는 값이라 admin-dong 고정 정보가 아니라 추천 응답 쪽에 있어야
합니다. `destAddress`를 보낸 경우에 한해 `recommendations[]`에 추가:

| 필드 | 타입 | 설명 |
|---|---|---|
| `route` | string | 경로 요약 (예: "6호선 증산역 → 디지털미디어시티역") |
| `transportType` | string | 카카오 step의 `type` 그대로 (`"SUBWAY"` \| `"BUS"`) |
| `lineNum` | string | 카카오 `vehicles[0].name` 그대로 (예: "6호선", "경의중앙선", "7021", "753") — 가공하지 않고 그대로 |
| `vehicleType` | string | 카카오 `vehicles[0].type` 그대로. 지하철이면 "일반"\|"급행", 버스면 "간선"\|"지선"\|"순환"\|"광역"\|"마을" |
| `walkMin` | number \| null | 도보 이동 시간(분) |
| `transitMin` | number \| null | 버스+지하철 등 차량 탑승 시간 합산(분). ~~`subwayMin`~~에서 이름 변경 — 지하철만 담는 것처럼 보이는 필드명이라 오해가 있었음(값 자체는 원래도 합산이었음) |

`lineColor`는 요청에서 뺐습니다 — 프론트가 `lineNum`(지하철)/`vehicleType`(버스)로 자체 매핑합니다.
`transferMin`(환승/대기 시간)은 별도 필드로 안 내려옵니다 — 아래 "결정된 사항" 참고.

사용처: [TabCommute.vue](src/recommendation/components/detail/TabCommute.vue)

> **결정된 사항(2026-08-19 논의)**
> - `transferMin`은 카카오 경로 응답에 대응 필드가 없어 역산하지 않고 **필드 자체를 뺀다**(option 1).
    >   부정확한 값을 굳이 만들지 않기로 함. 프론트는 이 필드가 없으면 도넛차트에서 해당 구간을 빼고,
    >   "환승 횟수"(`transferCount`, 이미 P0로 옴)만으로 표시한다.
> - `walkMin`/`subwayMin`은 카카오가 step을 안 쪼개는 경로가 있어 **null 가능**하다는 계약으로 간다.
    >   프론트는 null인 구간을 도넛에서 빼고, 전부 null이면 도넛 대신 안내 문구로 대체하도록 이미
    >   처리해뒀다([TabCommute.vue](src/recommendation/components/detail/TabCommute.vue) 참고).
> - `route`/`lineNum`이 없어도(목적지를 안 넣은 검색 등) 크래시 없이 "정보 없음"으로 표시하도록
    >   프론트에서 이미 방어 처리했다. 구현 순서에 영향 없음.

> **결정된 사항(2026-08-19 추가 논의, 배포 후 실데이터 검증)**
> - `subwayMin` → `transitMin`으로 필드명 변경 완료 반영. 값 의미는 그대로(버스+지하철 탑승
    >   시간 합산). 프론트 라벨은 `transportType`으로 이미 지하철/버스를 구분해서 보여주고
    >   있어서(TabCommute.vue), 굳이 "대중교통"으로 뭉뚱그리지 않고 기존처럼 "지하철 N분"/
    >   "버스 N분"으로 구분 표시하는 걸 유지했다.
> - 대기·환승 시간은 별도 필드로 안 내려오는 걸로 최종 확정. 프론트가
    >   `commuteTime - (walkMin ?? 0) - (transitMin ?? 0)`으로 역산한다. `walkMin`/`transitMin`이
    >   둘 다 null이면(정보가 아예 없으면) "전체가 대기시간"이라는 잘못된 값이 나오므로 그 경우엔
    >   역산하지 않고 기존 계약대로 도넛 대신 안내 문구를 보여준다. 결과가 0 이하로 나오면(데이터
    >   불일치 등) 표시하지 않는다.
> - `transportType`/`lineNum`/`vehicleType`이 환승 시 첫 번째 구간 기준인 것은 유지. "체감
    >   주 수단(가장 긴 구간) 기준으로 바꾸는 건" 별도 이슈로 분리하기로 함 — 지금은 대응 안 함.
> - 배포된 실제 응답으로 검증한 결과 `vehicleType`에 "직행"(직행좌석버스)이 추가로 확인되어
    >   프론트 색상 매핑([lineColors.js](src/recommendation/utils/lineColors.js))에 반영함.

> **결정된 사항(2026-08-19 논의, lineNum 원본 예시 공유받음)**
>
> 백엔드가 공유해준 카카오 길찾기 원본 예시:
> ```
> // 지하철 (routes[0].steps[0])
> "guidance": "6호선 (증산(명지대앞) > 디지털미디어시티)",
> "type": "SUBWAY",
> "vehicles": [{ "name": "6호선", "type": "일반" }]   // 다른 예: "경의중앙선", 급행이면 type: "급행"
>
> // 버스
> "vehicles": [{ "name": "7021", "type": "지선" }]
> "vehicles": [{ "name": "753", "type": "간선" }]
> ```
> 이 구조를 그대로 반영해서 위 표의 `transportType`/`lineNum`/`vehicleType`으로 확정했습니다.
> 프론트는 지하철이면 `lineNum`(예: "6호선")으로, 버스면 `vehicleType`(간선/지선/순환/광역/마을)으로
> 색상을 매핑합니다 — 버스는 노선 번호(`lineNum`)가 아니라 `vehicleType`으로 매핑하는 게 핵심입니다.

### P1-b. 행정동 고정 정보 — 신규 엔드포인트 — ✅ 완료

기존 `GET /api/admin-dongs?guName=&dongName=` (구+동 이름 → id 변환)과 짝을 이루는, id로 조회하는
API를 제안합니다.

```
GET /api/admin-dongs/batch?ids=1,2,3,4,5
```

응답:

```json
{
  "success": true,
  "data": [
    {
      "adminDongId": 1,
      "guName": "은평구",
      "dongName": "증산동",
      "lat": 37.5871,
      "lng": 126.9095,
      "deposit": 1000,
      "monthly": 64,
      "rentDist": [
        { "label": "50만원↓", "count": 3 },
        { "label": "50~60", "count": 5 },
        { "label": "60~70", "count": 7 },
        { "label": "70~80", "count": 2 },
        { "label": "80만원↑", "count": 1 }
      ],
      "cctv": 2.3,
      "police": "은평경찰서 증산지구대 (도보 8분)",
      "crimeRate": 3.2,
      "safetyScore": 78,
      "gyms": 5,
      "convenience": 7,
      "hospitals": 3,
      "parks": 2,
      "department": 0,
      "mart": 0
    }
  ],
  "error": null
}
```

| 필드 | 설명 | 사용처 |
|---|---|---|
| `deposit` / `monthly` | 보증금/월세 중위값(만원) | [TabHousing.vue](src/recommendation/components/detail/TabHousing.vue), [CompareTable.vue](src/recommendation/components/main/CompareTable.vue) |
| `rentDist` | 월세 구간별 매물 수 분포 | TabHousing.vue |
| `cctv` | 1000명당 CCTV 대수 등 지표 | [TabSafety.vue](src/recommendation/components/detail/TabSafety.vue) |
| `police` | 관할 지구대/파출소 + 도보시간 | TabSafety.vue |
| `crimeRate` | 범죄율 지표 | TabSafety.vue, CompareTable.vue |
| `safetyScore` | 안전 종합 점수(0~100) | TabSafety.vue, CompareTable.vue |
| `gyms` / `convenience` / `hospitals` / `parks` / `department` / `mart` | 반경 내 시설 개수 | [TabInfra.vue](src/recommendation/components/detail/TabInfra.vue) |

`ids`는 한 번에 추천 결과 최대 5개를 한꺼번에 조회하는 용도라 배치 조회로 요청드립니다(개별 호출
5번 대신).

## P2. 행정동별 장소 좌표 조회 (신규)

상세 화면의 통근/치안/생활 인프라 탭 안에 있는 지도([NeighborhoodMap.vue](src/recommendation/components/detail/NeighborhoodMap.vue))가 지금은 편의점·병원·지하철역 등 대부분을 프론트에서 카카오맵
SDK로 직접(반경 검색) 조회하고, 카카오에 없는 CCTV·가로등·안전비상벨만 프론트 mock으로 대체하고
있었습니다. `places` 테이블에 이미 행정동 단위로 다 있다고 하셔서, **DB category로 조회 가능한
장소만 지도에 표시**하는 걸로 전면 전환하려고 합니다(반경 검색보다 행정동 경계 기준이라 더
정확하고, mock도 없앨 수 있음).

```
GET /api/admin-dongs/{adminDongId}/places?categories=CONVENIENCE_STORE,CAFE,RESTAURANT,HOSPITAL,PHARMACY,GYM,BANK,PARK,DEPARTMENT_STORE,MART,POLICE,STREET_LIGHT,SAFETY_BELL,CCTV,BUS_STOP,SUBWAY_STATION
```

- `categories`는 선택 — 안 주면 전체 카테고리 반환. 상세 페이지에서 탭 열 때마다 그 동에 대해
  필요한 카테고리만 lazy하게 요청하고, 같은 동 안에서 탭을 옮겨다닐 땐 재호출하지 않고
  프론트에서 캐싱합니다.
- CCTV처럼 한 동에 몇 백 개씩 있을 수 있는 카테고리도 **응답에서 자르지 말고 전체를 주세요.**
  지도에 몇 개까지 찍을지/클러스터링할지는 프론트에서 최적화합니다.
- **`따릉이`/`택시승강장`은 화면에서 아예 뺍니다.** DB category enum에 대응 항목이 없어서,
  "오직 DB category로 조회 가능한 장소만 표시한다"는 원칙에 따라 제거합니다(지금처럼 카카오
  임시 데이터를 계속 쓰지 않음).

응답 예시:
```json
{
  "success": true,
  "data": [
    { "placeId": 123, "category": "CONVENIENCE_STORE", "name": "GS25 OO점", "lat": 37.5871, "lng": 126.9095 }
  ],
  "error": null
}
```

| 필드 | 설명 |
|---|---|
| `placeId` | `places.place_id` |
| `category` | `places.category` enum 값 그대로 |
| `name` | 장소명. 지도 마커 라벨/툴팁에 사용 |
| `lat` / `lng` | 좌표 |

화면 카테고리 ↔ DB category 매핑 (프론트에서 처리, 백엔드는 DB enum 그대로 내려주면 됨):

| 화면 표시 | DB `category` |
|---|---|
| 편의점 | `CONVENIENCE_STORE` |
| 카페/음식점 | `CAFE`, `RESTAURANT` (두 카테고리를 프론트에서 하나로 합쳐 표시) |
| 병원/약국 | `HOSPITAL`, `PHARMACY` (마찬가지로 합쳐서 표시) |
| 헬스장 | `GYM` |
| 은행 | `BANK` |
| 공원 | `PARK` |
| 백화점 | `DEPARTMENT_STORE` |
| 대형마트 | `MART` |
| CCTV | `CCTV` |
| 가로등 | `STREET_LIGHT` |
| 경찰서/지구대 | `POLICE` |
| 안전비상벨 | `SAFETY_BELL` |
| 지하철역 | `SUBWAY_STATION` |
| 버스정류장 | `BUS_STOP` |

`SPORTS`/`RIVER`/`TRAIL`/`CULTURE`/`TOWN_OFFICE`는 현재 화면에서 안 쓰고 있어 요청 대상에서
뺐습니다. 나중에 필요해지면 별도로 요청드리겠습니다.

## P3. 필터링 단계별 개수(퍼널) 추가 — 신규, 선택 사항 — 구조 협의 완료, 백엔드 구현 예정

로딩 화면 UX 개선 요청을 받아서, 지금처럼 고정된 문구를 순서대로 보여주는 대신 실제 필터링
단계별 개수를 보여주는 화면을 검토하고 있습니다.

실시간 스트리밍은 필요 없습니다 — 지금처럼 `POST /api/user-conditions` 응답을 한 번에 받은 뒤, 그
안에 담긴 숫자로 프론트에서 애니메이션만 재생하는 방식이면 충분합니다. 요청 바디 변경 없이 응답에
아래 필드만 추가해주시면 됩니다.

> **결정된 사항(2026-08-21 논의)**
>
> 처음 제안한 퍼널 구조(통근 → 예산 → 생활환경)가 실제 필터링 로직과 두 군데 달라서 백엔드
> 회신 기준으로 다시 정리했습니다.
> - **단계 순서가 반대입니다.** 실제 필터링 순서는 (선택한 구 →) **필수 인프라 → 예산/주거유형
    >   → 통근**입니다. 통근 조건은 카카오 API를 호출하는 가장 비용이 큰 단계라 일부러 마지막에
    >   검사합니다.
> - **"생활환경 비교"는 필터 단계가 아닙니다.** 우선순위 카테고리(생활 우선순위) 점수는
    >   후보를 걸러내는 게 아니라 순위만 매기는 로직이라 이 단계에서는 후보 개수가 줄지 않습니다.
    >   예산/통근을 통과한 후보 전체에 점수를 매긴 뒤 상위 5개를 최종 추천으로 뽑습니다. 그래서
    >   중간에 "생활환경 비교 N개" 같은 숫자는 없고, 예산/통근 통과 개수에서 바로 최종 5개로
    >   넘어갑니다.
> - `ESSENTIAL`(필수 시설) 단계는 포함하는 걸로 확정했습니다 — [StepPriority.vue](src/condition/components/StepPriority.vue)의
    >   "필수 조건"(반드시 가까이 있어야 하는 시설) 선택지가 요청 바디의 `essentialCategories`로
    >   이미 나가고 있어서, 백엔드가 제안한 5단계 구조를 그대로 반영합니다.
> - `label`은 "서울 전체" 대신 **"검색 대상"**으로 바꿉니다 — `guCodes`로 특정 구만 먼저 선택한
    >   검색이면 `TOTAL`이 426이 아니라 그 구들의 동 개수 합이 되므로, 두 경우 다 자연스러운
    >   문구로 통일합니다.
> - 후보가 중간에 0개가 되어 이후 단계 필터링 자체를 안 하는 경우, `destAddress`를 안 보낸
    >   검색의 `COMMUTE`처럼 남은 stage를 배열에서 빼서 주시는 걸로 일관되게 처리하기로
    >   했습니다 — 프론트도 배열에 없는 stage는 화면에서 건너뜁니다.

### 응답 예시 (추가되는 필드만 표시)

```json
{
  "success": true,
  "data": {
    "conditionId": 1,
    "filterFunnel": [
      { "stage": "TOTAL", "label": "검색 대상", "count": 426 },
      { "stage": "ESSENTIAL", "label": "필수 시설 조건 충족", "count": 190 },
      { "stage": "BUDGET", "label": "예산 조건 충족", "count": 27 },
      { "stage": "COMMUTE", "label": "출퇴근 가능", "count": 12 },
      { "stage": "FINAL", "label": "BEST", "count": 5 }
    ],
    "recommendations": [ ... ]
  },
  "error": null
}
```

| 필드 | 타입 | 설명 |
|---|---|---|
| `filterFunnel` | array | 필터링 파이프라인이 각 단계를 통과한 뒤 남은 동 개수. TOTAL부터 FINAL까지 순서대로. 후보가 0개가 돼서 실행되지 않은 이후 단계는 배열에서 빠짐 |
| `filterFunnel[].stage` | string | 단계 식별 코드. `TOTAL` \| `ESSENTIAL` \| `BUDGET` \| `COMMUTE` \| `FINAL` |
| `filterFunnel[].label` | string | 화면에 표시할 한글 라벨. 없어도 프론트에서 `stage` 기준으로 매핑 가능해서 선택 사항입니다 |
| `filterFunnel[].count` | number | 그 단계를 통과하고 남은 동 개수 |

### 참고

- `destAddress`를 안 보낸 검색이면 `COMMUTE` 단계가 배열에서 빠집니다. 프론트에서 그 스텝을
  건너뛰도록 처리하겠습니다.
- `guCodes`로 특정 구만 먼저 선택한 검색이면 `TOTAL.count`가 426이 아니라 그 구들의 동 개수
  합입니다.
- 마지막 stage(`FINAL`)의 `count`는 `recommendations.length`와 항상 같습니다.
- 기존 `conditionId`/`recommendations` 필드는 그대로 두고 `filterFunnel`만 추가하는 additive
  변경입니다. 우선순위가 높지 않아 다른 작업이 있으시면 뒤로 미루셔도 괜찮습니다.

## 참고: 이미 프론트에서 가정하고 진행한 부분

- `recommendationReason`/`caution`은 문서대로 콤마로 구분된 문자열을 그대로 파싱해서 카드의
  "추천 이유"/"유의사항" 목록으로 쓰고 있습니다. 구조화된 배열로 안 주셔도 됩니다.
- 요청 바디의 `guCodes`는 `public/gus.csv`에 있는 코드(예: 은평구="11120")로 보내고 있습니다.
  이 코드가 최근 한 번 바뀌어서(예전엔 은평구="11380") 프론트도 gus.csv 기준으로 다시
  맞췄습니다. 맞는지 확인 부탁드리고, 맞다면 이 문서(`API_USER_CONDITIONS.md`)에도 형식을
  명시해주시면 나중에 다른 사람이 봐도 헷갈리지 않을 것 같습니다.
