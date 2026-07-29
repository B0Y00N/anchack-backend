# Backend 코드 컨벤션 상세

이 문서는 [Backend 코드 컨벤션](CODE_CONVENTION.md)의 세부 기준, 적용 이유, 좋은 예시와 나쁜 예시를 설명합니다.

핵심 규칙은 `CODE_CONVENTION.md`를 우선하며, 이 문서는 규칙의 의도와 적용 방법을 이해하기 위한 참고 자료입니다.

> 이 문서의 모든 예시를 기계적으로 적용하지 않습니다. 현재 코드와 요구사항을 더 명확하고 안전하게 만드는 방향으로 사용합니다.

---

## 1. 컨벤션의 범위

컨벤션은 단순한 들여쓰기나 네이밍 규칙에만 한정되지 않습니다.

Backend 코드 컨벤션에는 다음 항목이 포함됩니다.

- 코드 포맷
- 네이밍
- 패키지 및 파일 구조
- 클래스와 메서드의 책임
- Spring 계층별 역할
- REST API 설계
- MyBatis와 SQL 작성
- 예외 처리와 로깅
- 보안
- 테스트
- 리팩토링
- 주석과 문서화

Git 브랜치, 커밋, Pull Request와 같은 협업 규칙은 조직의 `CONTRIBUTING.md`를 따릅니다.

---

## 2. 규칙 적용 수준

상세 규칙은 다음 세 단계로 해석합니다.

| 구분 | 의미 |
|---|---|
| 필수 | 특별한 사유가 없다면 반드시 적용 |
| 권장 | 가독성과 유지보수를 위해 우선 적용 |
| 선택 | 현재 문제를 해결하는 데 효과가 있을 때 적용 |

### 적용 원칙

1. 일관성은 개인의 선호보다 우선합니다.
2. 규칙을 지키기 위해 코드의 의도를 흐리지 않습니다.
3. 예외가 있다면 이유를 Pull Request에 남깁니다.
4. 팀에서 반복적으로 발생하는 문제는 문서에 새 규칙으로 추가합니다.
5. 실제로 사용하지 않는 규칙은 줄이거나 삭제합니다.

---

## 3. 코드 포맷 상세

### 3.1 들여쓰기

공백 4칸을 사용하고 탭 문자를 사용하지 않습니다.

```java
public RecommendationResponse recommend(
        RecommendationRequest request
) {
    validateRequest(request);

    return recommendationService.recommend(request);
}
```

여러 팀원의 IDE 설정이 다르면 같은 코드를 수정할 때 불필요한 공백 변경이 발생할 수 있습니다.  
가능하면 저장 시 코드 포맷과 import 정리를 실행하도록 설정합니다.

### 3.2 한 줄 길이

한 줄은 최대 120자를 기준으로 합니다.

한 줄 제한을 맞추기 위해 이름을 축약하지 않습니다.  
긴 표현은 매개변수, 메서드 체이닝, 조건식을 의미 단위로 나눕니다.

```java
// 지양
RecommendationResult result = recommendationService.createRecommendationResult(memberId, destination, maximumCommuteTime, maximumDeposit, maximumMonthlyRent);

// 권장
RecommendationResult result =
        recommendationService.createRecommendationResult(
                memberId,
                destination,
                maximumCommuteTime,
                maximumDeposit,
                maximumMonthlyRent
        );
```

메서드 추출이나 지역 변수를 사용하면 긴 줄과 복잡한 표현을 함께 줄일 수 있습니다.

### 3.3 중괄호

제어문의 본문이 한 줄이어도 중괄호를 사용합니다.

```java
// 지양
if (condition == null)
    throw new ConditionNotFoundException();

// 권장
if (condition == null) {
    throw new ConditionNotFoundException();
}
```

### 3.4 import

와일드카드 import를 사용하지 않습니다.

```java
// 지양
import java.util.*;

// 권장
import java.util.ArrayList;
import java.util.List;
```

정적 import는 테스트 assertion이나 의미가 분명한 상수처럼 가독성을 높일 때 사용합니다.  
어느 클래스의 멤버인지 알기 어려워진다면 사용하지 않습니다.

### 3.5 소스 파일

- 파일 인코딩은 UTF-8을 사용합니다.
- 하나의 Java 파일에는 하나의 최상위 타입만 둡니다.
- 파일 이름은 최상위 타입 이름과 일치시킵니다.
- 패키지 선언, import, 최상위 타입 순서로 작성합니다.

---

## 4. 네이밍 상세

좋은 이름은 주석 없이도 코드의 의도를 전달합니다.

### 4.1 클래스와 인터페이스

클래스는 명사 또는 명사구로 작성합니다.

```text
RecommendationService
NeighborhoodScore
CommuteRouteClient
ReviewAccessValidator
```

역할이 불분명한 이름은 피합니다.

```text
Manager
Processor
Handler
Helper
Util
Common
Data
Temp
```

이 접미사를 절대 금지하지는 않습니다.  
다만 `ReviewManager`처럼 이름만 보고 책임을 알 수 없다면 더 구체적으로 작성합니다.

```text
ReviewModerationService
ReviewAccessValidator
ReviewContentSanitizer
```

### 4.2 메서드

메서드는 동사 또는 동사구로 작성합니다.

```java
createReview()
findReviewById()
calculateRecommendationScore()
validateCommuteTime()
```

같은 의미에 여러 동사를 섞지 않습니다.

```java
// 지양
loadReview()
fetchReview()
searchReview()
selectReview()

// 권장
findReviewById()
```

#### 조회 메서드 의미

```java
Optional<Review> findById(Long reviewId);
Review getById(Long reviewId);
List<Review> findAllByAdminDongId(Long adminDongId);
boolean existsByUserIdAndAdminDongId(Long userId, Long adminDongId);
int countByStatus(ReviewStatus status);
```

팀에서 `find`와 `get`의 의미를 다르게 사용한다면 모든 기능에서 동일하게 유지합니다.

### 4.3 변수

변수 이름은 자료형이 아니라 역할을 표현합니다.

```java
// 지양
List<Review> reviewList;
String reviewString;
int reviewInt;

// 권장
List<Review> reviews;
String reviewContent;
int reviewCount;
```

불필요한 접두사와 헝가리안 표기법을 사용하지 않습니다.

```java
// 지양
strUserName
intReviewCount
listAdminDong

// 권장
userName
reviewCount
adminDongs
```

### 4.4 축약어

널리 사용되는 약어가 아니라면 줄여 쓰지 않습니다.

```java
// 지양
req
res
ctx
mgr
calc
rec

// 권장
request
response
context
manager
calculator
recommendation
```

`id`, `url`, `api`, `dto`, `sql`처럼 팀에서 공통으로 이해하는 약어는 사용할 수 있습니다.

### 4.5 Boolean

Boolean 이름은 질문처럼 읽혀야 합니다.

```java
boolean isActive;
boolean hasPermission;
boolean canModify;
boolean shouldRefresh;
boolean existsByEmail(String email);
```

부정 표현과 이중 부정을 피합니다.

```java
// 지양
boolean isNotInvalid;
boolean isNotDisabled;

// 권장
boolean isValid;
boolean isEnabled;
```

---

## 5. 기능 기준 패키지 구조 상세

안착 Backend는 최상위 패키지를 기능 기준으로 나눕니다.

```text
com.kbait.anchack
├── config
├── common
├── user
├── condition
├── recommendation
├── region
├── route
├── review
└── ingestion
```

### 5.1 기능 내부 구조

```text
review
├── controller
│   ├── ReviewController.java
│   └── moderation
│       └── ReviewModerationController.java
├── service
│   ├── ReviewService.java
│   └── impl
│       └── ReviewServiceImpl.java
├── mapper
│   ├── ReviewMapper.java
│   └── ReviewReportMapper.java
├── domain
│   ├── Review.java
│   └── ReviewReport.java
└── dto
    ├── ReviewCreateRequest.java
    ├── ReviewUpdateRequest.java
    └── ReviewResponse.java
```

### 5.2 기능 기준 구조의 목적

- 한 기능과 관련된 코드를 가까이 배치합니다.
- 담당자가 기능의 Controller, Service, Mapper, Domain, DTO를 빠르게 찾을 수 있습니다.
- 기능 삭제나 변경 시 영향 범위를 파악하기 쉽습니다.
- 최상위 `controller`, `service`, `mapper` 패키지가 지나치게 커지는 것을 방지합니다.

### 5.3 common 사용 기준

`common`은 여러 기능에서 실제로 공통 사용되는 코드만 둡니다.

```text
common
├── exception
├── response
└── util
```

다음 코드는 `common`으로 이동하지 않습니다.

- 한 기능에서만 사용하는 Validator
- 특정 외부 API에만 사용하는 Converter
- 특정 Domain에만 사용하는 상수
- 재사용 가능성이 있다는 예상만으로 분리한 코드

`util`에는 상태가 없고 특정 Domain에 속하지 않는 일반적인 기능만 둡니다.  
비즈니스 규칙을 `util`에 작성하지 않습니다.

### 5.4 config 관련 범위

기능 기준 패키지 구조에 맞춘 `RootConfig`, `ServletConfig`, Mapper Scan 변경은 별도 PR에서 진행합니다.

이 문서에서는 목표 패키지 구조만 정의하며, 설정 코드의 구체적인 구현은 현재 코드와 Spring Context 구성을 확인한 뒤 결정합니다.

---

## 6. 클래스 설계 상세

### 6.1 하나의 주요 책임

클래스는 하나의 주요 변경 이유를 가져야 합니다.

```java
// 지양: 추천 계산, 저장, 외부 API 호출, 응답 변환을 모두 담당
public class RecommendationServiceImpl {

    public RecommendationResponse recommend(
            RecommendationRequest request
    ) {
        // 주소 API 호출
        // 통근시간 API 호출
        // 점수 계산
        // DB 저장
        // 응답 생성
    }
}
```

책임을 의미 있는 단위로 분리합니다.

```text
RecommendationServiceImpl
WeightedScoreCalculator
CommuteRouteClient
RecommendationMapper
RecommendationResponseAssembler
```

클래스를 작게 만들기 위해 무조건 분리하지는 않습니다.  
변경 이유, 테스트 필요성, 재사용 여부가 분리 기준입니다.

### 6.2 인스턴스 변수

인스턴스 변수가 계속 늘어나면 책임이 많은지 확인합니다.

`인스턴스 변수 2개 이하` 같은 수치를 절대 규칙으로 적용하지 않습니다.  
다만 많은 의존성과 상태는 클래스가 여러 책임을 가진 신호일 수 있습니다.

### 6.3 변경 불가능한 상태

가능하면 생성 시점에 유효한 상태를 만들고, 이후 상태 변경을 제한합니다.

```java
public final class CommuteTime {

    private final int minutes;

    public CommuteTime(int minutes) {
        validate(minutes);
        this.minutes = minutes;
    }

    private void validate(int minutes) {
        if (minutes < 0) {
            throw new IllegalArgumentException(
                    "통근시간은 0분 이상이어야 합니다."
            );
        }
    }
}
```

### 6.4 상속과 조합

코드 재사용만을 위해 상속하지 않습니다.  
상속 관계가 실제로 `is-a` 관계인지 확인하고, 그렇지 않다면 조합을 우선 검토합니다.

---

## 7. 객체지향 설계 기준

객체지향 생활 체조의 원칙은 절대 규칙이 아니라 책임, 응집도, 결합도를 점검하는 기준으로 사용합니다.

### 7.1 들여쓰기 한 단계

메서드 안에서 들여쓰기 깊이를 줄이면 조건과 반복을 별도 책임으로 분리할 가능성이 높아집니다.

```java
// 지양
for (AdminDong adminDong : adminDongs) {
    if (adminDong.isWithinBudget()) {
        if (adminDong.isWithinCommuteTime()) {
            candidates.add(adminDong);
        }
    }
}
```

```java
// 권장
for (AdminDong adminDong : adminDongs) {
    addIfRecommendable(candidates, adminDong);
}

private void addIfRecommendable(
        List<AdminDong> candidates,
        AdminDong adminDong
) {
    if (!adminDong.isRecommendable()) {
        return;
    }

    candidates.add(adminDong);
}
```

한 단계만 반드시 허용하는 것은 아니지만 최대 2단계를 넘지 않도록 우선 개선합니다.

### 7.2 else 지양

Early Return으로 정상 흐름을 왼쪽에 유지할 수 있는지 확인합니다.

```java
// 지양
if (review == null) {
    throw new ReviewNotFoundException(reviewId);
} else {
    return ReviewResponse.from(review);
}
```

```java
// 권장
if (review == null) {
    throw new ReviewNotFoundException(reviewId);
}

return ReviewResponse.from(review);
```

두 조건이 대칭적이고 `else`가 더 명확한 경우에는 사용할 수 있습니다.

### 7.3 원시값과 문자열 포장

다음 조건에 해당하면 원시값 또는 문자열을 객체로 포장하는 것을 검토합니다.

- 자체 검증 규칙이 있음
- 단위와 의미가 중요함
- 여러 곳에서 같은 판단이 반복됨
- 잘못된 값이 생성되면 안 됨

```java
public final class MonthlyRent {

    private final int amount;

    public MonthlyRent(int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException(
                    "월세는 0원 이상이어야 합니다."
            );
        }

        this.amount = amount;
    }

    public boolean isWithin(int maximumAmount) {
        return amount <= maximumAmount;
    }
}
```

모든 숫자와 문자열을 무조건 포장하지는 않습니다.

### 7.4 한 줄에 하나의 점

`한 줄에 하나의 점`은 모든 메서드 체이닝을 금지한다는 뜻으로 적용하지 않습니다.  
객체 내부 구조를 연속해서 탐색하는 긴 체인을 줄이는 기준으로 사용합니다.

```java
// 지양
String guName = review.getAdminDong()
        .getGu()
        .getName()
        .trim();
```

```java
// 권장
String guName = review.getGuName();
```

Stream API, Builder, assertion처럼 체이닝이 의도를 명확하게 만드는 경우에는 허용합니다.

### 7.5 일급 컬렉션

컬렉션 자체에 검증 또는 비즈니스 규칙이 있다면 별도 클래스로 분리합니다.

```java
public final class RecommendationCandidates {

    private final List<AdminDong> values;

    public RecommendationCandidates(List<AdminDong> values) {
        this.values = List.copyOf(values);
    }

    public List<AdminDong> top(int count) {
        return values.stream()
                .limit(count)
                .toList();
    }
}
```

단순 전달 목적의 컬렉션까지 무조건 포장하지는 않습니다.

### 7.6 Getter와 Setter

Getter와 Setter를 무조건 금지하지 않습니다.

다만 다음 문제를 피합니다.

- 외부에서 상태를 직접 조합해 비즈니스 판단
- 검증 없이 객체 상태 변경
- 모든 필드에 자동으로 Setter 생성
- Domain 객체를 단순 데이터 주머니로 사용

```java
// 지양
review.setStatus(ReviewStatus.DELETED);
```

```java
// 권장
review.delete(requestUserId);
```

DTO와 MyBatis 매핑 등 기술적 필요가 있는 Getter·Setter는 사용할 수 있습니다.

---

## 8. 메서드 설계 상세

### 8.1 한 가지 주요 작업

메서드 내부에서 서로 다른 추상화 수준을 섞지 않습니다.

```java
// 지양
public RecommendationResponse recommend(
        RecommendationRequest request
) {
    validateRequest(request);

    String sql = "SELECT ...";
    // DB 연결 및 SQL 실행
    // 경로 API HTTP 요청
    // 점수 계산
    // 응답 JSON 생성
}
```

```java
// 권장
public RecommendationResponse recommend(
        RecommendationRequest request
) {
    validateRequest(request);

    List<AdminDong> candidates = findCandidates(request);
    List<RecommendationScore> scores =
            recommendationCalculator.calculate(candidates, request);

    saveRecommendation(scores);

    return RecommendationResponse.from(scores);
}
```

### 8.2 매개변수

매개변수가 많으면 관련 값이 하나의 개념인지 확인합니다.

```java
// 지양
recommend(
        Long memberId,
        String destination,
        int commuteMinutes,
        int deposit,
        int monthlyRent,
        List<Integer> weights
);
```

```java
// 권장
recommend(RecommendationRequest request);
```

단순히 매개변수 수를 줄이기 위해 관련 없는 값을 하나의 DTO로 묶지 않습니다.

### 8.3 부수 효과

조회처럼 보이는 메서드에서 상태를 변경하지 않습니다.

```java
// 지양
Review findReviewAndIncreaseViewCount(Long reviewId);
```

조회와 변경을 분리하거나 이름에서 변경 사실을 드러냅니다.

```java
Review findReviewById(Long reviewId);
void increaseViewCount(Long reviewId);
```

### 8.4 매직 넘버와 문자열

```java
// 지양
if (recommendations.size() > 10) {
    recommendations = recommendations.subList(0, 10);
}
```

```java
private static final int MAX_RECOMMENDATION_COUNT = 10;

if (recommendations.size() > MAX_RECOMMENDATION_COUNT) {
    recommendations =
            recommendations.subList(
                    0,
                    MAX_RECOMMENDATION_COUNT
            );
}
```

문맥상 의미가 분명한 `0`, `1`까지 모두 상수로 만들 필요는 없습니다.

---

## 9. Spring 계층별 책임 상세

### 9.1 Controller

Controller는 웹 계층의 어댑터입니다.

```java
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @PostMapping
    public ResponseEntity<ReviewResponse> create(
            @RequestBody ReviewCreateRequest request
    ) {
        ReviewResponse response =
                reviewService.create(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(response);
    }
}
```

Controller에서는 다음을 피합니다.

```java
// 지양
@PostMapping
public ReviewResponse create(
        @RequestBody ReviewCreateRequest request
) {
    if (request.getContent().length() > 1_000) {
        throw new IllegalArgumentException();
    }

    Review review = new Review();
    review.setContent(request.getContent());
    reviewMapper.insert(review);

    return ReviewResponse.from(review);
}
```

요청 형식 검증과 비즈니스 검증을 구분합니다.

- 형식 검증: null, 길이, 형식
- 비즈니스 검증: 작성 권한, 중복 작성, 상태 전이

### 9.2 Service 인터페이스와 구현체

```java
public interface ReviewService {

    ReviewResponse create(ReviewCreateRequest request);

    ReviewResponse findById(Long reviewId);
}
```

```java
@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;

    public ReviewServiceImpl(ReviewMapper reviewMapper) {
        this.reviewMapper = reviewMapper;
    }

    @Override
    public ReviewResponse create(
            ReviewCreateRequest request
    ) {
        // ...
    }
}
```

Controller는 구현체가 아니라 인터페이스에 의존합니다.

Service 인터페이스에 구현 세부사항을 노출하지 않습니다.

```java
// 지양
List<Map<String, Object>> selectReviewRows(
        Map<String, Object> parameter
);
```

```java
// 권장
List<ReviewResponse> findByAdminDongId(
        Long adminDongId
);
```

### 9.3 트랜잭션

트랜잭션은 Service 계층의 작업 단위에 적용합니다.

- 여러 쓰기 작업이 하나의 비즈니스 단위를 구성할 때 적용합니다.
- 조회 전용 작업은 읽기 전용 설정을 검토합니다.
- Controller와 Mapper에 트랜잭션 경계를 두지 않습니다.
- 외부 API 호출을 긴 DB 트랜잭션 안에 포함하지 않도록 검토합니다.

### 9.4 Mapper

Mapper 메서드는 SQL의 의도를 표현합니다.

```java
public interface ReviewMapper {

    Review findById(Long reviewId);

    List<Review> findAllByAdminDongId(
            Long adminDongId
    );

    int insert(Review review);

    int update(Review review);

    int deleteById(Long reviewId);
}
```

### 9.5 외부 API Client

```text
route
├── client
│   ├── RouteClient.java
│   └── impl
│       └── KakaoRouteClient.java
├── dto
├── domain
└── service
```

외부 API DTO와 내부 DTO를 구분합니다.

```text
KakaoRouteApiResponse
CommuteRoute
CommuteRouteResponse
```

외부 API 장애를 내부 예외로 변환합니다.

```java
try {
    return kakaoRouteApi.call(request);
} catch (ExternalApiException exception) {
    throw new RouteApiException(
            destination,
            exception
    );
}
```

---

## 10. Domain과 DTO 상세

### 10.1 Domain 객체

Domain 객체는 상태와 해당 상태에 대한 행동을 함께 가질 수 있습니다.

```java
public class Review {

    private Long reviewId;
    private Long userId;
    private ReviewStatus status;

    public boolean isWrittenBy(Long userId) {
        return this.userId.equals(userId);
    }

    public void delete(Long requestUserId) {
        if (!isWrittenBy(requestUserId)) {
            throw new ReviewAccessDeniedException(reviewId);
        }

        status = ReviewStatus.DELETED;
    }
}
```

MyBatis 매핑 요구사항에 따라 기본 생성자나 Setter가 필요한 경우 팀의 매핑 방식에 맞게 사용할 수 있습니다.  
기술적 요구 때문에 Domain의 모든 상태를 외부에 공개하지는 않습니다.

### 10.2 Request DTO

Request DTO는 외부 입력을 표현합니다.

```java
public class ReviewCreateRequest {

    private Long adminDongId;
    private String content;

    public Long getAdminDongId() {
        return adminDongId;
    }

    public String getContent() {
        return content;
    }
}
```

Request DTO에는 복잡한 비즈니스 로직을 넣지 않습니다.

### 10.3 Response DTO

Response DTO는 API 계약에 필요한 값만 포함합니다.

```java
public class ReviewResponse {

    private Long reviewId;
    private String nickname;
    private String content;
    private LocalDateTime createdAt;
}
```

다음 값을 무분별하게 노출하지 않습니다.

- 비밀번호
- 내부 상태 코드
- 삭제 여부를 판단하기 위한 내부 컬럼
- 관리용 메모
- 외부 API 원본 응답

---

## 11. REST API 상세

### 11.1 URI

```text
GET    /api/admin-dongs
GET    /api/admin-dongs/{adminDongId}
GET    /api/admin-dongs/{adminDongId}/reviews

POST   /api/user-conditions
GET    /api/user-conditions/{conditionId}
PATCH  /api/user-conditions/{conditionId}
DELETE /api/user-conditions/{conditionId}

POST   /api/recommendations
GET    /api/recommendations/{recommendationId}

POST   /api/reviews
PATCH  /api/reviews/{reviewId}
DELETE /api/reviews/{reviewId}
```

### 11.2 HTTP Method

| Method | 사용 목적 |
|---|---|
| GET | 리소스 조회 |
| POST | 새 리소스 생성 또는 계산 요청 |
| PUT | 리소스 전체 교체 |
| PATCH | 리소스 일부 변경 |
| DELETE | 리소스 삭제 |

추천 실행처럼 계산 결과를 생성하고 저장하는 기능은 `POST /api/recommendations`처럼 표현할 수 있습니다.

### 11.3 Path와 Query Parameter

리소스 식별자는 Path Variable을 사용합니다.

```text
GET /api/reviews/{reviewId}
```

검색, 정렬, 필터, 페이지 조건은 Query Parameter를 사용합니다.

```text
GET /api/reviews?admin-dong-id=10&page=1&size=20
```

### 11.4 상태 코드

| 상태 | 예시 |
|---|---|
| 200 OK | 조회, 수정 성공 |
| 201 Created | 생성 성공 |
| 204 No Content | 응답 본문 없는 삭제 성공 |
| 400 Bad Request | 요청값 형식 또는 비즈니스 조건 오류 |
| 401 Unauthorized | 인증 필요 |
| 403 Forbidden | 권한 없음 |
| 404 Not Found | 리소스 없음 |
| 409 Conflict | 중복 또는 상태 충돌 |
| 500 Internal Server Error | 처리되지 않은 서버 오류 |

팀의 공통 응답 형식과 상태 코드를 API 명세서에 함께 기록합니다.

---

## 12. MyBatis 상세

### 12.1 인터페이스와 XML 일치

```java
public interface ReviewMapper {

    Review findById(Long reviewId);
}
```

```xml
<select
    id="findById"
    parameterType="long"
    resultType="com.kbait.anchack.review.domain.Review"
>
    SELECT
        review_id,
        user_id,
        admin_dong_id,
        content,
        created_at
    FROM reviews
    WHERE review_id = #{reviewId}
</select>
```

### 12.2 SELECT 컬럼 명시

`SELECT *`는 다음 문제를 만들 수 있습니다.

- 필요하지 않은 컬럼 조회
- 테이블 변경 시 예상하지 못한 매핑
- SQL만 보고 반환 컬럼을 알기 어려움
- 같은 이름의 컬럼이 있는 JOIN에서 충돌 가능성

따라서 필요한 컬럼을 직접 작성합니다.

### 12.3 `#{}`와 `${}`

`#{}`는 PreparedStatement 파라미터 바인딩을 사용합니다.

```xml
WHERE user_id = #{userId}
```

`${}`는 문자열을 SQL에 직접 삽입하므로 사용자 입력값에 사용하지 않습니다.

```xml
<!-- 위험 -->
ORDER BY ${sortColumn}
```

동적 정렬 컬럼이 필요하다면 서버의 Enum이나 화이트리스트로 허용 값을 제한합니다.

```java
public enum ReviewSort {

    LATEST("created_at"),
    SCORE("score");

    private final String column;
}
```

### 12.4 동적 SQL

동적 조건은 읽기 쉽게 구성합니다.

```xml
<select id="findAll" resultType="Review">
    SELECT
        review_id,
        user_id,
        admin_dong_id,
        content
    FROM reviews
    <where>
        <if test="adminDongId != null">
            admin_dong_id = #{adminDongId}
        </if>
        <if test="userId != null">
            AND user_id = #{userId}
        </if>
    </where>
</select>
```

조건이 복잡해지면 Query Parameter DTO를 사용합니다.

### 12.5 N+1 형태의 반복 조회

```java
// 지양
for (AdminDong adminDong : adminDongs) {
    List<Review> reviews =
            reviewMapper.findAllByAdminDongId(
                    adminDong.getAdminDongId()
            );
}
```

필요한 데이터를 한 번에 조회하거나 배치 조회합니다.

```java
List<Review> reviews =
        reviewMapper.findAllByAdminDongIds(adminDongIds);
```

---

## 13. 예외 처리 상세

### 13.1 구체적인 예외

```text
MemberNotFoundException
ReviewNotFoundException
ReviewAccessDeniedException
InvalidRecommendationConditionException
ExternalRouteApiException
```

예외 이름과 메시지는 문제 상황을 설명해야 합니다.

### 13.2 공통 예외 처리

Controller마다 같은 `try-catch`를 작성하지 않습니다.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handle(
            BusinessException exception
    ) {
        // ...
    }
}
```

### 13.3 예외 변환

저수준 예외를 그대로 외부에 노출하지 않습니다.

```java
try {
    return routeClient.findRoute(request);
} catch (HttpClientException exception) {
    throw new ExternalRouteApiException(exception);
}
```

원인을 추적할 수 있도록 원본 예외를 보존합니다.

### 13.4 catch

```java
// 지양
try {
    process();
} catch (Exception exception) {
}
```

예외를 처리할 수 없다면 상위 계층으로 전달합니다.  
복구할 수 있다면 복구 전략을 명확히 구현합니다.

---

## 14. Null과 Optional

### 14.1 컬렉션

컬렉션 결과가 없으면 빈 컬렉션을 반환합니다.

```java
return Collections.emptyList();
```

`null`과 빈 컬렉션을 서로 다른 의미로 사용하지 않습니다.

### 14.2 Optional

단일 조회 결과가 없을 수 있음을 표현할 때 사용할 수 있습니다.

```java
Optional<Review> findById(Long reviewId);
```

다음 위치에는 남용하지 않습니다.

- Domain 필드
- Request·Response DTO 필드
- 메서드 매개변수
- 컬렉션 대신 사용

프로젝트의 MyBatis 매핑 방식과 예외 처리 흐름을 고려하여 `Optional` 사용 여부를 팀에서 일관되게 유지합니다.

---

## 15. 로깅 상세

### 15.1 로그 수준

| 수준 | 용도 |
|---|---|
| ERROR | 요청 처리가 실패한 예외 |
| WARN | 복구했지만 확인이 필요한 상황 |
| INFO | 주요 비즈니스 흐름과 상태 변경 |
| DEBUG | 개발과 문제 분석을 위한 상세 정보 |

### 15.2 파라미터 치환

```java
// 지양
log.info(
        "recommendationId=" + recommendationId
);

// 권장
log.info(
        "추천 생성 완료. recommendationId={}",
        recommendationId
);
```

### 15.3 중복 로그

같은 예외를 Client, Service, Controller Advice에서 모두 기록하지 않습니다.  
어느 계층에서 최종 기록할지 정합니다.

### 15.4 민감 정보

다음 값은 로그에 남기지 않습니다.

- 비밀번호
- Access Token
- Refresh Token
- API Key
- 전체 주소 등 불필요한 개인정보
- DB 접속 비밀번호

---

## 16. 보안 기본 규칙 상세

- 비밀값을 코드와 Git에 커밋하지 않습니다.
- 실제 설정 파일은 `.gitignore`로 제외합니다.
- 예제 설정 파일에는 키 이름만 남기고 값을 비웁니다.
- 사용자 입력값을 서버에서 검증합니다.
- 수정·삭제 시 리소스 소유권을 확인합니다.
- 화면에서 버튼을 숨기는 것만으로 권한을 보호하지 않습니다.
- MyBatis 값 바인딩에는 `#{}`를 사용합니다.
- CORS 허용 Origin은 환경별로 관리합니다.
- 외부 API 응답을 신뢰하지 않고 필수값과 범위를 확인합니다.

```properties
jdbc.url=
jdbc.username=
jdbc.password=
external.route.api-key=
```

---

## 17. 테스트 상세

### 17.1 한 테스트에 한 케이스

하나의 테스트는 하나의 주요 조건과 결과를 검증합니다.

```java
@Test
void 리뷰_작성자가_아니면_삭제할_수_없다() {
    // given
    Review review = createReview(WRITER_ID);

    // when & then
    assertThatThrownBy(
            () -> review.delete(OTHER_USER_ID)
    ).isInstanceOf(ReviewAccessDeniedException.class);
}
```

한 테스트에서 서로 다른 기능을 여러 개 검증하지 않습니다.

### 17.2 Given-When-Then

- Given: 테스트에 필요한 상태 준비
- When: 검증할 행동 실행
- Then: 결과 검증

모든 테스트에 주석을 강제하지 않습니다.  
코드 구조만으로 단계가 명확하다면 주석을 생략할 수 있습니다.

### 17.3 실행 구문

하나의 테스트에서 주요 실행 흐름은 하나로 유지합니다.

```java
RecommendationResult result =
        recommendationService.recommend(request);
```

테스트 준비를 위해 여러 메서드를 호출하는 것은 허용합니다.  
실행 구문을 한 물리적 줄로 강제하기보다 하나의 주요 행동을 검증하는 것이 목적입니다.

### 17.4 테스트의 if문

테스트 안에서 조건에 따라 assertion을 바꾸지 않습니다.

```java
// 지양
if (result.isSuccess()) {
    assertThat(result.getValue()).isNotNull();
}
```

테스트 조건을 분리합니다.

```java
@Test
void 유효한_조건이면_추천_결과를_반환한다() {
    // ...
}

@Test
void 유효하지_않은_조건이면_예외가_발생한다() {
    // ...
}
```

### 17.5 매개변수화 테스트

입력값만 다르고 검증 목적이 같다면 사용합니다.

```java
@ParameterizedTest
@ValueSource(ints = {-1, -10, -100})
void 통근시간이_음수이면_예외가_발생한다(
        int commuteMinutes
) {
    assertThatThrownBy(
            () -> new CommuteTime(commuteMinutes)
    ).isInstanceOf(IllegalArgumentException.class);
}
```

### 17.6 private 메서드

private 메서드를 직접 테스트하지 않습니다.  
공개 메서드의 동작을 통해 간접적으로 검증합니다.

private 메서드 테스트가 필요하다고 느껴진다면 별도 책임으로 분리할 수 있는지 검토합니다.

### 17.7 테스트 이름

팀에서 한글 또는 영문 방식을 정하고 한 프로젝트 안에서 일관되게 사용합니다.

```java
void 예산을_초과한_동네는_추천_후보에서_제외한다()
```

```java
void excludesNeighborhoodWhenBudgetIsExceeded()
```

---

## 18. 리팩토링 상세

리팩토링은 기능을 추가하는 작업이 아니라 기존 동작을 유지하면서 내부 구조를 개선하는 작업입니다.

### 18.1 핵심 원칙

> 작게 변경하고, 동작을 확인한 뒤, 다음 단계로 진행합니다.

### 18.2 진행 순서

1. 현재 동작을 이해합니다.
2. 필요한 테스트를 추가합니다.
3. 작은 범위를 메서드로 분리합니다.
4. 컴파일과 테스트를 실행합니다.
5. 이름과 책임을 개선합니다.
6. 중복을 제거합니다.
7. 다시 테스트합니다.
8. 다음 범위로 확장합니다.

### 18.3 기능 추가와 분리

```text
test: 추천 점수 계산 기존 동작 테스트 추가
refactor: 추천 점수 계산 로직 메서드 분리
refactor: 추천 계산 책임을 별도 클래스로 이동
feat: 새로운 가중치 계산 방식 추가
```

기능 추가와 구조 변경을 분리하면 오류가 발생했을 때 원인을 추적하기 쉽습니다.

### 18.4 큰 변경을 피하는 이유

한 번에 전체 구조를 변경하면 다음 문제가 발생합니다.

- 오류가 발생한 위치를 찾기 어려움
- 기존 버그와 새로 발생한 버그를 구분하기 어려움
- 리뷰 범위가 지나치게 커짐
- 롤백하기 어려움
- 변경 목적이 섞임

### 18.5 리팩토링 신호

다음 상황에서 리팩토링을 검토합니다.

- 중복 코드
- 지나치게 긴 메서드
- 지나치게 많은 매개변수
- 변경 이유가 여러 개인 클래스
- 기능과 맞지 않는 클래스 이름
- 반복되는 조건문
- 과도한 Getter 기반 판단
- 서로 다른 계층의 책임 혼합
- 테스트하기 어려운 코드

---

## 19. 성능 관련 기준

측정 없이 예상만으로 복잡한 최적화를 적용하지 않습니다.

### 기본 원칙

1. 먼저 올바르게 동작하도록 구현합니다.
2. 실제 데이터와 측정 결과로 문제를 확인합니다.
3. 병목 지점을 중심으로 개선합니다.
4. 개선 전후를 비교합니다.

### 확인 항목

- 반복문 안의 DB 호출
- 반복되는 외부 API 호출
- 필요하지 않은 컬럼 조회
- 제한 없는 전체 데이터 조회
- 동일 데이터 반복 조회
- 캐시의 만료 및 갱신 정책
- 인덱스가 필요한 검색 조건
- 대량 INSERT·UPDATE 방식

성능 개선 PR에는 가능하면 다음 내용을 작성합니다.

- 기존 문제
- 측정 방법
- 변경 전 결과
- 변경 후 결과
- 부작용과 제한사항

---

## 20. 주석과 Javadoc 상세

### 20.1 좋은 주석

코드로 알기 어려운 이유와 제약사항을 설명합니다.

```java
// 외부 경로 API의 동일 목적지 호출 제한으로 캐시를 우선 조회한다.
CommuteRoute route =
        commuteRouteCache.find(destination);
```

### 20.2 나쁜 주석

코드를 그대로 한국어로 반복합니다.

```java
// 추천 결과를 저장한다.
recommendationMapper.insert(recommendation);
```

### 20.3 TODO

```java
// TODO: #42 행정동 경계 변경 시 캐시 갱신
```

Issue 번호 없이 작성된 TODO가 장기간 남지 않도록 합니다.

### 20.4 주석 처리된 코드

사용하지 않는 코드는 Git 이력으로 복구할 수 있으므로 삭제합니다.

```java
// oldRecommendationService.calculate();
// recommendationMapper.legacyInsert();
```

### 20.5 Javadoc

다음 상황에서 작성합니다.

- 공개 API의 사용 방법이 명확하지 않음
- 특별한 제약조건이 있음
- 단위나 범위가 이름만으로 드러나지 않음
- 예외 발생 조건을 사용자가 알아야 함

자명한 Getter와 Setter에 설명을 반복하는 Javadoc은 작성하지 않습니다.

---

## 21. 좋은 코드와 나쁜 코드를 판단하는 기준

좋은 코드는 단순히 짧은 코드가 아닙니다.

다음 질문으로 판단합니다.

- 이름만 보고 의도를 이해할 수 있는가?
- 한 곳을 변경했을 때 영향 범위를 예상할 수 있는가?
- 잘못된 상태가 만들어지는 것을 방지하는가?
- 테스트하기 쉬운가?
- 계층과 클래스의 책임이 분명한가?
- 오류가 발생했을 때 원인을 찾을 수 있는가?
- 현재 요구사항보다 지나친 추상화를 만들지 않았는가?

### 지나친 추상화

```java
// 실제 구현이 하나뿐이고 변화 가능성도 없는데
// 여러 단계의 Factory, Strategy, Provider를 미리 생성
```

확장 가능성만을 이유로 구조를 복잡하게 만들지 않습니다.

### 중복과 추상화

비슷해 보인다는 이유만으로 즉시 하나로 합치지 않습니다.  
두 코드가 같은 이유로 변경되는지 확인한 뒤 추상화합니다.

### DRY와 가독성

중복 제거가 코드의 의미를 숨긴다면 작은 중복을 허용할 수 있습니다.  
팀원이 이해하기 쉬운 구조를 우선합니다.

---

## 22. 설정 클래스 관련 보류 사항

기능 기준 패키지 구조를 적용하면 컴포넌트 스캔과 Mapper Scan 설정을 함께 검토해야 합니다.

현재 문서에서는 다음 사항만 합의합니다.

- 최상위 패키지는 기능 기준으로 나눕니다.
- 각 기능 내부에서 Controller, Service, Mapper, Domain, DTO를 구분합니다.
- 모든 Service는 인터페이스와 구현체를 분리합니다.
- 구체적인 `RootConfig`, `ServletConfig`, `MapperScan` 변경은 별도 PR에서 처리합니다.

설정 변경 PR에서는 다음을 확인합니다.

- Root Context와 Servlet Context의 Bean 중복 등록 여부
- 기능 패키지 아래 Controller 탐색 여부
- 기능 패키지 아래 Service 구현체 탐색 여부
- Mapper 인터페이스 탐색 여부
- Mapper XML 경로와 namespace 일치 여부
- 테스트 실행 시 Context 로딩 여부

---

## 23. 참고 자료

이 문서는 아래 자료의 모든 규칙을 그대로 적용하지 않습니다.  
안착 프로젝트의 기술 스택과 팀 합의에 맞게 필요한 원칙을 선별했습니다.

### 공식 문서와 스타일 가이드

- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Oracle Java Code Conventions](https://www.oracle.com/java/technologies/javase/codeconventions-contents.html)
- [Java Language Specification: Names](https://docs.oracle.com/javase/specs/jls/se24/html/jls-6.html)
- [Spring Framework Reference Documentation](https://docs.spring.io/spring-framework/reference/)
- [Spring Framework: Classpath Scanning](https://docs.spring.io/spring-framework/reference/core/beans/classpath-scanning.html)
- [Spring Framework: Dependency Injection](https://docs.spring.io/spring-framework/reference/core/beans/dependencies/factory-collaborators.html)
- [MyBatis Reference Documentation](https://mybatis.org/mybatis-3/)
- [MyBatis Mapper XML](https://mybatis.org/mybatis-3/sqlmap-xml.html)
- [MyBatis-Spring Mapper Scanning](https://mybatis.org/spring/mappers.html)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)

### 참고 서적

- [Effective Java, 3rd Edition — Joshua Bloch](https://www.informit.com/store/effective-java-9780134685991)
- [Clean Code — Robert C. Martin](https://www.informit.com/store/clean-code-a-handbook-of-agile-software-craftsmanship-9780136083221)
- [Refactoring, 2nd Edition — Martin Fowler](https://martinfowler.com/books/refactoring.html)
- [Good Code, Bad Code — Tom Long](https://www.manning.com/books/good-code-bad-code)
