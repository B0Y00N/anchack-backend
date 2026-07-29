# Backend 코드 컨벤션

안착 Backend 코드의 일관성, 가독성, 유지보수성을 높이기 위한 핵심 규칙입니다.

이 문서는 Java, Spring Framework, Spring MVC, MyBatis를 사용하는 Backend Repository에 적용합니다.  
세부 설명과 좋은 예시·나쁜 예시는 [Backend 코드 컨벤션 상세](CODE_CONVENTION_DETAIL.md)에서 확인합니다.

> 컨벤션의 목적은 규칙을 늘리는 것이 아니라, 팀원이 다른 사람의 코드를 빠르게 이해하고 안전하게 수정할 수 있도록 공통 기준을 만드는 것입니다.

---

## 1. 적용 원칙

- 본 문서의 규칙을 팀 공통 기준으로 사용합니다.
- 규칙을 적용했을 때 코드가 더 복잡해진다면 적용 이유를 다시 검토합니다.
- 예외가 필요한 경우 Pull Request에 이유를 작성하고 팀원과 논의합니다.
- 설정 클래스의 컴포넌트 스캔 변경은 패키지 구조 합의 이후 별도 작업으로 진행합니다.

---

## 2. 기본 코드 스타일

- 들여쓰기는 공백 4칸을 사용합니다.
- 탭 문자를 사용하지 않습니다.
- 파일 인코딩은 UTF-8을 사용합니다.
- 한 줄은 최대 120자를 기준으로 합니다.
- `if`, `else`, `for`, `while`의 본문이 한 줄이어도 중괄호를 사용합니다.
- 한 줄에는 하나의 문장만 작성합니다.
- 와일드카드 import를 사용하지 않습니다.
- 사용하지 않는 import, 변수, 메서드, 주석을 제거합니다.
- 한 Java 파일에는 하나의 최상위 타입만 선언합니다.
- 변경되지 않는 필드는 가능한 한 `final`로 선언합니다.

```java
// 지양
import java.util.*;

if (member == null)
    return;

// 권장
import java.util.List;

if (member == null) {
    return;
}
```

---

## 3. 네이밍 규칙

| 대상 | 규칙 | 예시 |
|---|---|---|
| 패키지 | 영문 소문자 | `com.kbait.anchack.review` |
| 클래스·인터페이스 | PascalCase | `RecommendationService` |
| 메서드·변수 | camelCase | `calculateRecommendationScore` |
| 상수 | UPPER_SNAKE_CASE | `MAX_RECOMMENDATION_COUNT` |
| DB 테이블·컬럼 | snake_case | `admin_dong_id` |

### 이름 작성 기준

- 클래스는 명사 또는 명사구로 작성합니다.
- 메서드는 동사 또는 동사구로 작성합니다.
- 이름만 보고 역할과 의미를 파악할 수 있도록 작성합니다.
- 널리 알려진 표현이 아니라면 축약하지 않습니다.
- 컬렉션 변수는 복수형으로 작성합니다.
- Boolean 값은 `is`, `has`, `can`, `should`, `exists` 등으로 의미를 드러냅니다.
- 같은 동작에는 같은 동사를 사용합니다.

```java
// 지양
List<Review> list;
String data;
int cnt;
Recommendation rec;

// 권장
List<Review> reviews;
String adminDongCode;
int recommendationCount;
Recommendation recommendation;
```

### 조회 메서드 이름

| 접두사 | 의미 |
|---|---|
| `find` | 결과가 없을 수 있는 조회 |
| `get` | 반드시 존재해야 하며 없으면 예외 |
| `findAll` | 여러 결과 조회 |
| `exists` | 존재 여부 조회 |
| `count` | 개수 조회 |

---

## 4. 패키지 구조

최상위 패키지는 **기능을 기준으로 분리**하고, 각 기능 내부에서 계층을 구분합니다.

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

기능 패키지는 다음 구조를 기본으로 합니다.

```text
review
├── controller
├── service
│   ├── ReviewService.java
│   └── impl
│       └── ReviewServiceImpl.java
├── mapper
├── domain
└── dto
```

### 패키지 규칙

- 특정 기능에서만 사용하는 코드는 해당 기능 패키지에 둡니다.
- 실제로 여러 기능에서 공통 사용하는 코드만 `common`에 둡니다.
- 편의를 이유로 모든 코드를 `common` 또는 `util`에 모으지 않습니다.
- 패키지 간 순환 의존성을 만들지 않습니다.
- Mapper XML은 Java Mapper와 동일한 기능 기준으로 구성합니다.
- 현재 설정 클래스 변경은 별도 PR에서 처리합니다.

---

## 5. 계층별 책임

기본 의존 방향은 다음과 같습니다.

```text
Controller
    ↓
Service
    ↓
Domain / Mapper / External Client
    ↓
Database / External API
```

### Controller

Controller는 HTTP 요청과 응답을 담당합니다.

- 요청값 수신 및 검증
- 인증된 사용자 정보 확인
- Service 호출
- HTTP 상태와 응답 반환

Controller에는 다음 내용을 작성하지 않습니다.

- 핵심 비즈니스 로직
- 데이터베이스 직접 접근
- 추천 점수 계산
- 복잡한 데이터 조합
- 긴 반복문과 조건문

### Service

모든 Service는 인터페이스와 구현체를 분리합니다.

```text
ReviewService
ReviewServiceImpl
```

- Controller는 Service 인터페이스에 의존합니다.
- 구현체는 `service.impl` 패키지에 배치합니다.
- 구현체 이름은 `기능명ServiceImpl`로 작성합니다.
- 구현체에 `@Service`를 선언합니다.
- Service는 비즈니스 규칙, 작업 흐름, 트랜잭션 경계를 담당합니다.

### Mapper

Mapper는 데이터베이스 접근만 담당합니다.

- SQL 실행
- 파라미터 전달
- 조회 결과 매핑

Mapper에는 비즈니스 판단이나 HTTP 관련 코드를 작성하지 않습니다.

### External Client

주소·지도·경로 등 외부 API 호출 코드는 별도 Client로 분리합니다.

- 외부 API 응답 DTO를 내부 응답 DTO로 그대로 사용하지 않습니다.
- 외부 API의 오류와 변경이 내부 로직에 직접 전파되지 않도록 변환합니다.

---

## 6. Domain과 DTO

### Domain

`domain` 패키지에는 해당 기능의 상태와 비즈니스 동작을 표현하는 객체를 둡니다.

- Domain 객체는 필요한 검증과 상태 판단을 스스로 수행하도록 작성합니다.
- Domain 객체를 API 응답으로 직접 반환하지 않습니다.
- 무조건적인 Getter와 Setter 생성을 피합니다.
- 외부에서 값을 꺼내 판단하기보다 객체에 메시지를 보내는 방식을 우선 검토합니다.

```java
// 지양
if (review.getWriterId().equals(userId)) {
    // ...
}

// 권장
if (review.isWrittenBy(userId)) {
    // ...
}
```

### DTO

요청 DTO와 응답 DTO를 분리합니다.

```text
ReviewCreateRequest
ReviewUpdateRequest
ReviewResponse
```

- DTO 이름에는 사용 목적이 드러나야 합니다.
- Domain 객체를 요청·응답 DTO로 그대로 사용하지 않습니다.
- 데이터베이스 조회 객체를 API 응답으로 직접 반환하지 않습니다.
- DTO 대신 `Map<String, Object>`를 사용하지 않습니다.
- 비밀번호나 내부 관리 정보처럼 필요하지 않은 값을 응답에 포함하지 않습니다.

---

## 7. 의존성 주입

필수 의존성은 생성자 주입을 사용합니다.

```java
@Service
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;

    public ReviewServiceImpl(ReviewMapper reviewMapper) {
        this.reviewMapper = reviewMapper;
    }
}
```

- 필드 주입을 사용하지 않습니다.
- 필수 의존성에 Setter 주입을 사용하지 않습니다.
- 생성자 매개변수가 지나치게 많다면 클래스의 책임이 과도한지 확인합니다.
- 클래스 내부에서 의존 객체를 직접 생성하지 않습니다.

---

## 8. 메서드와 제어문

- 한 메서드는 하나의 주요 작업을 수행합니다.
- 메서드 이름과 실제 구현 내용이 일치해야 합니다.
- 설명이 필요한 코드 블록은 의미 있는 메서드로 분리합니다.
- 매개변수가 많아지면 요청 객체 도입을 검토합니다.
- 들여쓰기 깊이는 한 단계 수준을 목표로 하고 최대 2단계를 넘기지 않도록 합니다.
- `else`보다 Early Return으로 더 단순하게 표현할 수 있는지 먼저 확인합니다.
- 중첩 삼항 연산자를 사용하지 않습니다.
- 의미가 있는 숫자와 문자열은 상수로 관리합니다.
- 긴 메서드 체이닝은 중간 변수나 메서드로 분리합니다.

```java
// 지양
if (review != null) {
    if (review.isWrittenBy(userId)) {
        return ReviewResponse.from(review);
    }
}

// 권장
if (review == null) {
    throw new ReviewNotFoundException(reviewId);
}

if (!review.isWrittenBy(userId)) {
    throw new ReviewAccessDeniedException(reviewId);
}

return ReviewResponse.from(review);
```

---

## 9. REST API

URI는 동작이 아니라 리소스를 표현합니다.

- URI에는 명사를 사용합니다.
- 컬렉션 리소스는 복수형을 사용합니다.
- URI는 영문 소문자와 하이픈을 사용합니다.
- 행위는 HTTP Method로 표현합니다.
- URI 끝에 `/`를 붙이지 않습니다.
- 검색과 필터 조건은 Query Parameter를 사용합니다.
- 리소스 관계가 명확할 때만 중첩 경로를 사용합니다.

```text
GET    /api/reviews
GET    /api/reviews/{reviewId}
POST   /api/reviews
PATCH  /api/reviews/{reviewId}
DELETE /api/reviews/{reviewId}
```

```text
# 지양
GET  /api/getReviewList
POST /api/createReview
POST /api/deleteReview
```

---

## 10. MyBatis와 SQL

Mapper 인터페이스의 메서드 이름과 Mapper XML의 SQL `id`를 동일하게 작성합니다.

- `SELECT *`를 사용하지 않습니다.
- 필요한 컬럼을 명시합니다.
- SQL 키워드는 대문자로 작성합니다.
- 테이블과 컬럼은 `snake_case`를 사용합니다.
- Java와 XML의 파라미터 이름을 동일하게 작성합니다.
- 값 바인딩에는 `#{}`를 사용합니다.
- 사용자 입력값에 `${}`를 사용하지 않습니다.
- 하나의 Mapper에는 해당 기능과 관련된 SQL만 작성합니다.
- 반복문 안에서 DB를 반복 호출하지 않습니다.

```sql
-- 지양
SELECT *
FROM reviews;
```

```sql
-- 권장
SELECT
    review_id,
    user_id,
    admin_dong_id,
    content,
    created_at
FROM reviews
WHERE review_id = #{reviewId};
```

`${}`가 반드시 필요하다면 서버에서 허용한 값만 사용하고, Enum 또는 화이트리스트로 검증합니다.

---

## 11. 예외, 로깅, 보안

### 예외 처리

- 문제 상황을 나타내는 구체적인 예외를 사용합니다.
- 빈 `catch`문을 작성하지 않습니다.
- 예외를 잡은 뒤 아무 처리 없이 무시하지 않습니다.
- 클라이언트 오류와 서버 내부 오류를 구분합니다.
- 공통 예외 처리기를 사용합니다.
- 내부 구현 정보와 민감한 정보를 응답에 노출하지 않습니다.

```java
// 지양
throw new RuntimeException("오류 발생");

// 권장
throw new ReviewNotFoundException(reviewId);
```

### 로깅

- `System.out.println`을 사용하지 않습니다.
- 문자열 연결 대신 로깅 프레임워크의 파라미터 치환 방식을 사용합니다.
- 같은 예외를 여러 계층에서 반복해서 기록하지 않습니다.
- 로그만 보고 문제 상황을 식별할 수 있는 최소 정보를 남깁니다.

### 보안

다음 정보는 코드와 로그에 남기지 않습니다.

- 비밀번호
- 인증 토큰
- API Key
- 개인정보
- 데이터베이스 접속 정보

사용자 입력값을 신뢰하지 않고 서버에서 검증합니다.  
수정·삭제 기능은 화면 표시 여부와 관계없이 서버에서 권한과 소유권을 검증합니다.

---

## 12. 테스트

모든 코드를 TDD로 작성하는 것은 강제하지 않습니다.  
다만 결과가 명확한 핵심 비즈니스 로직에는 단위 테스트를 작성합니다.

### 우선 테스트 대상

- 추천 점수 계산
- 사용자 가중치 적용
- 예산 및 통근시간 조건 판별
- 입력값 검증
- 리뷰 수정·삭제 권한
- 외부 API 응답 변환
- 예외 발생 조건

### 테스트 규칙

- 한 테스트는 하나의 주요 케이스를 검증합니다.
- 테스트끼리 실행 순서에 의존하지 않습니다.
- 테스트에서 `if`문을 사용하지 않습니다.
- 입력값만 다른 동일한 검증은 매개변수화 테스트를 검토합니다.
- private 메서드를 직접 테스트하지 않습니다.
- 구현 세부사항보다 공개된 동작을 검증합니다.
- 테스트 이름으로 조건과 예상 결과를 이해할 수 있도록 작성합니다.

```java
@Test
void 예산을_초과한_동네는_추천_후보에서_제외한다() {
    // given
    RecommendationCondition condition = createCondition();

    // when
    List<AdminDong> result = recommendationService.findCandidates(condition);

    // then
    assertThat(result).doesNotContain(overBudgetAdminDong);
}
```

---

## 13. 리팩토링

리팩토링은 외부 동작을 유지하면서 내부 구조를 개선하는 작업입니다.

> 동작은 유지하고, 구조만 개선합니다.

- 기능 추가와 대규모 리팩토링을 한 PR에 섞지 않습니다.
- 한 번에 전체 구조를 변경하지 않습니다.
- 작은 단위로 변경하고 매 단계 동작을 확인합니다.
- 기존 동작을 보호할 테스트가 없다면 먼저 테스트를 추가합니다.
- 구조 개선 후 중복 제거와 이름 개선을 점진적으로 진행합니다.

```text
기존 동작 확인
    ↓
테스트 추가
    ↓
작은 범위 변경
    ↓
컴파일·테스트
    ↓
다음 범위 변경
```

---

## 14. 주석과 문서화

- 코드가 무엇을 하는지 그대로 반복하는 주석을 작성하지 않습니다.
- 코드만으로 알기 어려운 이유와 제약사항을 작성합니다.
- TODO에는 관련 Issue 번호를 함께 작성합니다.
- 사용하지 않는 코드를 주석으로 보관하지 않습니다.
- AI가 생성한 설명이나 대화 흔적을 남기지 않습니다.
- 사용 방법과 제약을 코드만으로 이해하기 어려운 공개 API에 Javadoc을 작성합니다.

```java
// 지양: 리뷰를 조회한다.
Review review = reviewMapper.findById(reviewId);

// 권장: 경로 API 호출량을 줄이기 위해 동일 목적지는 캐시를 우선 조회한다.
CommuteRoute route = commuteRouteCache.find(destination);
```

---

## 15. PR 전 확인사항

### 코드

- [ ] 코드 포맷과 import 정리를 적용했습니다.
- [ ] 와일드카드 import를 사용하지 않았습니다.
- [ ] 이름만 보고 역할을 이해할 수 있습니다.
- [ ] 사용하지 않는 코드와 주석을 제거했습니다.
- [ ] 매직 넘버와 문자열을 확인했습니다.
- [ ] 메서드와 클래스가 과도한 책임을 갖지 않습니다.
- [ ] 중첩 조건문을 단순화했습니다.
- [ ] 불필요한 Getter와 Setter를 만들지 않았습니다.

### 구조와 API

- [ ] 기능 기준 패키지 구조를 따릅니다.
- [ ] Controller가 Service 인터페이스에 의존합니다.
- [ ] Service 인터페이스와 구현체를 분리했습니다.
- [ ] Controller가 Mapper를 직접 호출하지 않습니다.
- [ ] Service에 HTTP 객체가 포함되지 않았습니다.
- [ ] 요청 DTO와 응답 DTO를 구분했습니다.
- [ ] URI에 불필요한 동사를 사용하지 않았습니다.

### Database

- [ ] `SELECT *`를 사용하지 않았습니다.
- [ ] 사용자 입력값에 `${}`를 사용하지 않았습니다.
- [ ] Java와 Mapper XML의 파라미터 이름이 일치합니다.
- [ ] 반복문 안에서 DB를 반복 호출하지 않습니다.

### 예외와 보안

- [ ] 예외 상황을 구체적으로 처리했습니다.
- [ ] 빈 `catch`문이 없습니다.
- [ ] 민감한 정보가 코드와 로그에 포함되지 않았습니다.
- [ ] 권한과 소유권을 서버에서 검증했습니다.

### 테스트와 문서

- [ ] 핵심 비즈니스 로직의 테스트를 작성했습니다.
- [ ] 한 테스트에서 하나의 주요 케이스를 검증합니다.
- [ ] private 메서드를 직접 테스트하지 않습니다.
- [ ] 관련 문서를 함께 수정했습니다.
- [ ] AI가 작성한 코드를 직접 이해하고 검토했습니다.

---

## 참고 자료

- [Backend 코드 컨벤션 상세](CODE_CONVENTION_DETAIL.md)
- [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- [Spring Framework Reference Documentation](https://docs.spring.io/spring-framework/reference/)
- [MyBatis Reference Documentation](https://mybatis.org/mybatis-3/)
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
