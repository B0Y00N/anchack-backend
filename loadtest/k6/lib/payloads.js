// UserConditionCreateRequest 페이로드 빌더.
//
// rentalType/commuteType/essentialCategories/preferredHouseTypes는 DB ENUM(한글)이 아니라
// ConditionServiceImpl의 RENTAL_TYPE_CODES/COMMUTE_TYPE_CODES/ESSENTIAL_CATEGORY_CODES/
// HOUSE_TYPE_CODES가 정의한 영문 코드로 보내야 한다 - 한글을 그대로 보내면
// "지원하지 않는 값입니다" 400으로 거부된다. 실제로 curl로 검증한 값들:
//   rentalType: MONTHLY|JEONSE, commuteType: PUBLIC_TRANSIT|CAR
//   essentialCategories: CONVENIENCE_STORE|GYM|HOSPITAL|PARK|MART
//   preferredHouseTypes: OFFICETEL|VILLA|DETACHED|MULTI_HOUSEHOLD|APARTMENT|ONE_ROOM
//
// 값 목록은 로컬 복제 DB 실측 기준(2026-08-21)이다 - 다른 DB로 다시 세팅하면 재확인:
//   SELECT gu_code, name FROM gus;
//   SELECT DISTINCT category FROM places;                (essentialCategories가 매핑하는 대상)
//   SELECT house_type, COUNT(*) FROM property_metrics GROUP BY house_type;
//
// **중요**: 로컬 복제 DB의 property_metrics.house_type은 실제로
// '오피스텔'/'다세대'/'연립'/'연립다세대'만 존재하는데, 앱이 지원하는 HOUSE_TYPE_CODES
// (HouseTypeBudgetFilter.ALL_HOUSE_TYPES 기준: 오피스텔/빌라/단독/다가구/아파트/원룸)와
// 교집합이 "오피스텔"(OFFICETEL) 하나뿐이다. 다른 house_type을 preferredHouseTypes로
// 보내면 HouseTypeBudgetFilter가 후보를 매번 0개로 걸러버려서 CommuteFilter/스코어링까지
// 못 가고 요청이 "가짜로" 가벼워진다 - 부하테스트 결과가 왜곡되므로 OFFICETEL만 쓴다.

const GU_CODES = ['11010', '11020', '11030', '11040', '11050']; // 종로구/중구/용산구/성동구/광진구

const PRIORITY_CATEGORIES = [
  'TRANSIT', 'SAFETY', 'SPORTS', 'FOOD', 'CONVENIENCE', 'HEALTHCARE', 'CULTURE', 'NATURE', 'SILENCE',
];

const ESSENTIAL_CATEGORIES = ['CONVENIENCE_STORE', 'GYM', 'HOSPITAL', 'PARK', 'MART'];

// property_metrics와 실제 교집합이 있는 유일한 값 (위 주석 참고) - 배열이지만 항상 이거 하나만 씀
const HOUSE_TYPES = ['OFFICETEL'];

const RENTAL_TYPES = ['MONTHLY', 'JEONSE'];

function pick(arr, n) {
  const copy = arr.slice();

  for (let i = copy.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    const tmp = copy[i];
    copy[i] = copy[j];
    copy[j] = tmp;
  }

  return copy.slice(0, Math.min(n, copy.length));
}

function pickOne(arr) {
  return arr[Math.floor(Math.random() * arr.length)];
}

/** destAddress 없이 guCodes 기반으로 필터링하는 기본 조건 (카카오/RouteService 자체를 안 탐). */
export function buildConditionRequest(overrides) {
  const base = {
    destAddress: null,
    guCodes: pick(GU_CODES, 2),
    commuteType: 'PUBLIC_TRANSIT',
    maxCommuteTime: 60,
    maxTransferCount: 3,
    priorityCategories: pick(PRIORITY_CATEGORIES, 3),
    essentialCategories: pick(ESSENTIAL_CATEGORIES, 2),
    rentalType: pickOne(RENTAL_TYPES),
    maxDeposit: 500000000,
    maxRent: 2000000,
    preferredHouseTypes: HOUSE_TYPES,
    minArea: 10,
  };

  return Object.assign(base, overrides || {});
}

/**
 * destAddress를 채운 조건 - CommuteFilter의 geocode+병렬호출 경로까지 탄다.
 * ROUTE_MODE=stub이 아니면 이 요청은 카카오 실API를 호출하니 주의.
 */
export function buildConditionRequestWithDest(destAddress, overrides) {
  return buildConditionRequest(Object.assign({ destAddress, guCodes: [] }, overrides || {}));
}
