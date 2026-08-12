# 사용자 조건 등록 & 추천 API

프론트 5단계 조건 입력 마법사("동네 찾기 시작" 버튼)가 호출하는 API. 조건 저장과 추천
계산을 하나의 동기 요청으로 처리한다.

## `POST /api/user-conditions`

### 인증

`Authorization: Bearer {accessToken}` 헤더 필수. 없거나 유효하지 않으면 `401`.

### Request Body

| 필드 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `destAddress` | string \| null | X | 목적지 주소. "아직 정해지지 않았어요" 선택 시 `null` |
| `guCodes` | string[] | X | 목적지 대신 지역구로 검색할 때 사용, 0~2개. `destAddress`와 동시에 값을 채우지 않음(프론트 탭 UI가 상호배타적으로 입력을 막음). 0개면 전체 행정동이 후보 |
| `commuteType` | string | O | `"PUBLIC_TRANSIT"` \| `"CAR"` |
| `maxCommuteTime` | number | X | 분 단위 (예: `60`) |
| `maxTransferCount` | number | X | 환승 횟수 (예: `2`) |
| `priorityCategories` | string[] | O | 1~3개, **순서가 1순위→3순위를 의미**(가중치 계산에 순서 자체가 쓰임). 값은 [우선순위 카테고리 코드](#우선순위-카테고리-코드) 참고 |
| `essentialCategories` | string[] | X | 0개 이상. 값은 [필수 인프라 코드](#필수-인프라-코드) 참고 |
| `rentalType` | string | O | `"MONTHLY"`(월세) \| `"JEONSE"`(전세) |
| `maxDeposit` | number | X | **만원 단위** (예: `3000` = 3,000만원). 서버에서 원 단위로 변환해 저장 |
| `maxRent` | number | X | **만원 단위** (예: `70` = 70만원) |
| `preferredHouseTypes` | string[] | X | 0개 이상. 값은 [주거 유형 코드](#주거-유형-코드) 참고 |
| `minArea` | number | X | 최소 전용면적(㎡) |

`title`은 이 요청에 포함하지 않는다 — 조건을 "저장"할 때(추후 별도 API) 입력받아 채운다.

### 코드 목록

#### 우선순위 카테고리 코드
`condition_weights`/`recommendation_scores`와 동일한 값을 그대로 사용(별도 매핑 없음).

| 코드 | 의미 |
|---|---|
| `TRANSIT` | 교통 |
| `SAFETY` | 치안 |
| `SPORTS` | 운동 |
| `FOOD` | 식생활 |
| `CONVENIENCE` | 편의시설 |
| `HEALTHCARE` | 의료 |
| `CULTURE` | 문화생활 |
| `NATURE` | 자연환경 |
| `SILENCE` | 조용한 동네 |

#### 필수 인프라 코드

| 코드 | 의미 |
|---|---|
| `CONVENIENCE_STORE` | 편의점 |
| `GYM` | 헬스장 |
| `HOSPITAL` | 병원 |
| `PARK` | 공원 |
| `MART` | 대형마트 |

#### 주거 유형 코드

| 코드 | 의미 |
|---|---|
| `OFFICETEL` | 오피스텔 |
| `VILLA` | 빌라 |
| `DETACHED` | 단독 |
| `MULTI_HOUSEHOLD` | 다가구 |
| `APARTMENT` | 아파트 |
| `ONE_ROOM` | 원룸 |

> ⚠️ **프론트 확인 필요**: 현재 화면엔 "단독·다가구"가 버튼 하나로 묶여 있는데, 이 API는
> `DETACHED`/`MULTI_HOUSEHOLD`를 별도 코드로 받는다. 두 값 중 하나만 보내거나, 두 개를
> 배열에 함께 담아 보내면 됨(둘 다 선택한 것으로 처리하고 싶다면 배열에 두 코드 모두 포함).
> 또한 현재 화면의 "최대 관리비" 입력은 이 API가 받지 않으니(백엔드 스키마에서 제거됨)
> 요청에서 빼야 함.

### Request 예시

```json
{
  "destAddress": null,
  "guCodes": [],
  "commuteType": "PUBLIC_TRANSIT",
  "maxCommuteTime": 60,
  "maxTransferCount": 2,
  "priorityCategories": ["SAFETY", "SPORTS", "FOOD"],
  "essentialCategories": ["CONVENIENCE_STORE", "GYM"],
  "rentalType": "MONTHLY",
  "maxDeposit": 3000,
  "maxRent": 70,
  "preferredHouseTypes": ["OFFICETEL"],
  "minArea": 20
}
```

### Response — 성공 (`200 OK`)

```json
{
  "success": true,
  "data": {
    "conditionId": 1,
    "recommendations": [
      {
        "adminDongId": 1,
        "totalScore": 60.00,
        "dataCoverageRate": 100.00,
        "rank": 1,
        "commuteTime": null,
        "transferCount": null,
        "recommendationReason": "편의시설이 많아요, 조용한 분위기가 있어요, 범죄율이 낮아요",
        "caution": "대중교통 접근성이 떨어질 수 있어요, 상권과 거리가 있어요"
      }
    ]
  },
  "error": null
}
```

| 필드 | 설명 |
|---|---|
| `conditionId` | 생성된 조건 ID. 추후 "조건 저장" API 등에서 사용 |
| `recommendations` | `totalScore` 내림차순, **최대 5개** |
| `recommendations[].adminDongId` | 행정동 ID. 동네 이름/구 등 표시용 정보는 아직 이 응답에 없음 — 별도 행정동 조회 API 필요(추후 제공 예정) |
| `recommendations[].totalScore` | 0~100 |
| `recommendations[].dataCoverageRate` | 0~100(%). 선택한 우선순위 카테고리 중 실제 데이터가 있었던 비율 |
| `recommendations[].commuteTime` / `transferCount` | `destAddress`를 안 보냈으면 `null` |
| `recommendations[].recommendationReason` / `caution` | OpenAI(프록시)로 생성한 문장. 각각 쉼표로 구분된 짧은 문구 1~3개. 서버가 stub 모드로 떠 있으면(배포 환경에 따라 다름) 대신 고정 문구("추후 openai api 호출")가 내려올 수 있음 |

### Response — 실패

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "COMMON_INVALID_REQUEST",
    "message": "priorityCategories: size must be between 1 and 3"
  }
}
```

| HTTP Status | `error.code` | 상황 |
|---|---|---|
| 401 | `AUTH_UNAUTHORIZED` | 인증 토큰 없음/무효 |
| 400 | `COMMON_INVALID_REQUEST` | 필드 형식 오류(필수값 누락, 범위 초과 등) 또는 코드 목록에 없는 값 전달 |
| 500 | `COMMON_INTERNAL_ERROR` | 서버 내부 오류 |

## 미확정/추후 반영 사항

- 표시용 행정동 정보(이름/구/좌표 등) 조회 API — 별도 도메인에서 추후 제공
- 조건 "저장"(제목 입력 + `is_saved=true` 전환) API
