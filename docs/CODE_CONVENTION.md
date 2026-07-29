# Backend 코드 컨벤션

안착 Backend Repository에 적용하는 규칙입니다.

브랜치·커밋·PR·보안 등 협업 규칙은 조직의 [CONTRIBUTING.md](https://github.com/kb-a-it/.github/blob/main/CONTRIBUTING.md)를 따르고, 이 문서는 Java·Spring Framework·MyBatis 작성 기준만 다룹니다.

이 문서에는 **이 프로젝트에서만 참인 규칙**을 담습니다. 일반적인 Java 코드 작성법은 10장의 스타일 가이드에 위임합니다.

---

## 1. 스택과 제약

**Spring Boot가 아닙니다.** Legacy Spring + WAR 구성입니다. 아래를 반드시 지켜주세요.

| 항목 | 사용 |
|---|---|
| Java | 17 |
| Spring Framework | 5.3.37 (Boot 아님) |
| 영속성 | MyBatis 3.5.16 + MyBatis-Spring 2.1.2 (JPA·Spring Data 사용하지 않음) |
| 서블릿 API | `javax.*` (`jakarta.*` 아님) |
| 빌드·배포 | Gradle, WAR, 외부 Tomcat |
| 설정 | Java Config (`RootConfig`, `ServletConfig`) |

다음은 사용하지 않습니다. AI 도구로 코드를 생성하면 자주 섞여 들어오니 특히 주의해주세요.

- `@SpringBootApplication`, `@SpringBootTest`, `spring-boot-starter-*`
- `application.yml` (설정은 `application.properties`)
- `jakarta.validation`, `jakarta.servlet` 등 `jakarta.*` 패키지
- `JpaRepository`, `@Entity`

## 2. 패키지 구조

최상위 패키지는 **기능 기준**으로 나누고, 각 기능 안에서 계층을 구분합니다. — 담당 기능의 코드를 한곳에서 찾고 삭제·변경 시 영향 범위를 파악하기 위함입니다.

```text
com.kbait.anchack                    review
├── config                           ├── controller
├── common                           ├── service
├── user                             │   ├── ReviewService.java
├── condition                        │   └── impl
├── recommendation                   │       └── ReviewServiceImpl.java
├── region                           ├── mapper
├── route                            ├── domain
├── review                           └── dto
└── ingestion
```

- 특정 기능에서만 쓰는 코드는 해당 기능 패키지에 둡니다.
- `common`에는 **실제로 두 개 이상의 기능이 함께 쓰는 코드만** 둡니다. 재사용할 것 같다는 예상만으로 옮기지 않습니다.
- 패키지 간 순환 의존성을 만들지 않습니다.
- Mapper XML도 같은 기능 기준으로 배치합니다.

> 컴포넌트 스캔과 MapperScan 변경 전까지는 기존 패키지 구조로 작업합니다. (11장 참고)

## 3. 계층 규칙

```text
Controller → Service → Domain / Mapper / Client → DB / 외부 API
```

**Controller** — HTTP 입출력만 담당합니다. 비즈니스 로직, DB 직접 접근, 점수 계산, 긴 반복문을 두지 않습니다.

**Service** — 비즈니스 규칙과 트랜잭션 경계를 담당합니다.

- **모든 Service는 인터페이스와 구현체를 분리합니다.** 구현체는 `service.impl`에 두고 이름은 `기능명ServiceImpl`로 작성합니다. — 여러 명이 동시에 작업할 때 인터페이스를 계약으로 먼저 확정하기 위함입니다.
- Controller는 구현체가 아니라 인터페이스에 의존합니다.
- `@Service`와 `@Transactional`은 **구현체**에 선언합니다. — 인터페이스에 선언하면 프록시 생성 방식에 따라 적용되지 않을 수 있습니다.
- 인터페이스에 `Map<String, Object>` 같은 구현 세부사항을 노출하지 않습니다.
- 외부 API 호출을 긴 DB 트랜잭션 안에 넣지 않습니다.

**Mapper** — SQL 실행과 결과 매핑만 담당합니다. 비즈니스 판단과 HTTP 관련 코드를 넣지 않습니다.

**Client** — 외부 API(주소·지도·경로) 호출을 분리합니다. 외부 응답 DTO를 내부 응답으로 그대로 쓰지 않고, 외부 예외를 내부 예외로 변환합니다. — 외부 API 변경이 내부 로직으로 번지지 않도록 하기 위함입니다.

## 4. 이름

| 대상 | 규칙 | 예시 |
|---|---|---|
| 패키지 | 영문 소문자 | `com.kbait.anchack.review` |
| 클래스·인터페이스 | PascalCase | `RecommendationService` |
| 메서드·변수 | camelCase | `calculateRecommendationScore` |
| 상수 | UPPER_SNAKE_CASE | `MAX_RECOMMENDATION_COUNT` |
| DB 테이블·컬럼 | snake_case | `admin_dong_id` |

- 널리 쓰이는 약어가 아니면 줄이지 않습니다. (`req`, `cnt`, `rec` 지양)
- 같은 동작에는 같은 동사를 씁니다. `load`·`fetch`·`select`를 섞지 않습니다.

**조회 메서드 접두사**

| 접두사 | 의미 |
|---|---|
| `find` | 결과가 없을 수 있음 |
| `get` | 반드시 존재하며 없으면 예외 |
| `findAll` | 여러 건 조회 |
| `exists` | 존재 여부 |
| `count` | 개수 |

**Mapper는 `find` 계열만 사용합니다.** 결과가 없을 때 예외로 바꾸는 책임은 Service가 가집니다. — Mapper는 예외를 던지지 않기 때문입니다.

```java
// ReviewServiceImpl
private Review getReview(Long reviewId) {
    Review review = reviewMapper.findById(reviewId);

    if (review == null) {
        throw new ReviewNotFoundException(reviewId);
    }

    return review;
}
```

## 5. Lombok

| 어노테이션 | 사용 기준 |
|---|---|
| `@Getter` | Domain·DTO 모두 사용 |
| `@RequiredArgsConstructor` | 생성자 주입에 사용 |
| `@NoArgsConstructor` | MyBatis·Jackson이 인스턴스를 생성해야 할 때 |
| `@Builder` | 생성 파라미터가 많은 DTO |
| `@Setter` | 필드 단위로만 선언 |
| `@ToString` | 필요한 필드만 지정 |
| `@Data`, `@Value` | 사용하지 않음 |

- `@Data`는 `@Setter`·`@EqualsAndHashCode`·`@ToString`을 한 번에 만듭니다. — 의도하지 않은 상태 변경과 연관 객체 순환 참조가 생기므로 필요한 것만 개별 선언합니다.
- 의존성 주입은 `@RequiredArgsConstructor` + `private final`을 사용합니다. 필드 주입(`@Autowired`)을 쓰지 않습니다.
- **MyBatis가 매핑하는 클래스는 `@NoArgsConstructor`가 필요합니다.** — MyBatis와 Jackson은 기본 생성자로 객체를 만든 뒤 값을 채우므로, `final` 필드와 `@RequiredArgsConstructor`만 있으면 매핑되지 않습니다.
- `@ToString`에 비밀번호·토큰 필드를 포함하지 않습니다. — 로그에 그대로 노출됩니다.

## 6. Domain과 DTO

- 요청 DTO와 응답 DTO를 분리하고 이름에 목적을 드러냅니다. (`ReviewCreateRequest`, `ReviewResponse`)
- Domain 객체와 DB 조회 결과를 API 응답으로 직접 반환하지 않습니다. — DB 스키마 변경이 API 계약을 깨뜨리지 않도록 하기 위함입니다.
- DTO 대신 `Map<String, Object>`를 쓰지 않습니다.
- 비밀번호·내부 상태 코드·관리용 값을 응답에 포함하지 않습니다.
- 금액은 원 단위 정수로 다루고, **보증금은 `long`**, 월세·관리비는 `int`를 씁니다. — `int` 최대값이 약 21억이라 전세 보증금에서 넘칠 수 있습니다.
- 상태 판단은 값을 꺼내지 말고 객체에 물어봅니다. (`review.getWriterId().equals(userId)` → `review.isWrittenBy(userId)`)

## 7. API 규약

- URI는 명사·복수형으로 쓰고 행위는 HTTP Method로 표현합니다.
- **경로 세그먼트는 하이픈**(`/api/admin-dongs`), **Query Parameter는 camelCase**(`?adminDongId=10`)를 씁니다. — Spring MVC는 하이픈이 포함된 파라미터를 Java 필드에 자동 바인딩하지 못하고, `@ModelAttribute` DTO 바인딩은 아예 불가능합니다.

```text
GET /api/reviews          GET /api/reviews/{reviewId}       POST /api/reviews
PATCH /api/reviews/{reviewId}                               DELETE /api/reviews/{reviewId}
```

**모든 응답은 `ApiResponse<T>`로 감쌉니다.** HTTP 상태 코드도 그대로 사용합니다.

```json
{ "success": true,  "data": { "reviewId": 1 }, "error": null }
{ "success": false, "data": null, "error": { "code": "REVIEW_NOT_FOUND", "message": "리뷰를 찾을 수 없습니다." } }
```

- `error.code`는 `{도메인}_{사유}` 형식의 대문자 스네이크로 작성하고, **`common/exception/ErrorCode` enum 한 곳에서만 정의합니다.** 문자열 리터럴을 직접 쓰지 않습니다. — 담당자마다 다른 코드가 생기지 않도록 하기 위함입니다.
- `error.message`는 사용자에게 그대로 보여줄 수 있는 문장으로 쓰고 내부 구현 정보를 담지 않습니다.
- 목록 응답의 페이징 정보(`page`, `size`, `totalElements`)는 `data` 안에 넣습니다.

**형식 검증은 `@Valid`와 Bean Validation으로 처리합니다.** Controller에서 `if`로 직접 검사하지 않습니다. 작성 권한·중복 작성·상태 전이 같은 비즈니스 검증은 Service가 담당합니다.

## 8. MyBatis

- **`mybatis-config.xml`에 `mapUnderscoreToCamelCase`를 `true`로 설정하고, SQL에서 컬럼 별칭을 쓰지 않습니다.** — 이 설정이 없으면 `admin_dong_id`가 `adminDongId`에 매핑되지 않고 예외 없이 `null`이 됩니다.
- `SELECT *`를 쓰지 않고 필요한 컬럼을 명시합니다. — 컬럼이 추가되면 매핑이 조용히 깨집니다.
- Mapper 인터페이스의 메서드 이름과 XML의 `id`, 파라미터 이름을 일치시킵니다.
- 값 바인딩은 `#{}`를 씁니다. **사용자 입력값에 `${}`를 쓰지 않습니다.** — SQL Injection이 발생합니다. 동적 정렬 컬럼이 필요하면 Enum이나 화이트리스트로 허용 값을 제한합니다.
- 반복문 안에서 DB를 반복 호출하지 않습니다. 한 번에 조회하거나 배치로 조회합니다.
- 조인 결과처럼 자동 매핑으로 표현하기 어려운 경우에만 `<resultMap>`을 씁니다.

## 9. 예외·로깅·보안

- 문제 상황을 나타내는 구체적인 예외를 씁니다. `throw new RuntimeException("오류")`를 쓰지 않습니다.
- 빈 `catch`를 두지 않고, 처리할 수 없으면 상위로 전달합니다.
- 예외 응답은 `@RestControllerAdvice` 공통 처리기에서 `ApiResponse`로 반환합니다. Controller마다 `try-catch`를 반복하지 않습니다.
- 저수준 예외를 그대로 노출하지 않고 내부 예외로 변환하되, 원인 추적을 위해 원본 예외를 보존합니다.
- `System.out.println` 대신 로그를 쓰고, 파라미터 치환(`log.info("... id={}", id)`)을 사용합니다.
- 같은 예외를 여러 계층에서 중복 기록하지 않습니다.
- **로그에 비밀번호·토큰·API Key·전체 주소를 남기지 않습니다.**
- **수정·삭제는 화면 표시 여부와 관계없이 서버에서 권한과 소유권을 검증합니다.**

> 그 밖의 보안 규칙(비밀값 커밋 금지, `.example` 파일 관리 등)은 [CONTRIBUTING.md](https://github.com/kb-a-it/.github/blob/main/CONTRIBUTING.md) 9장을 따릅니다.

## 10. 테스트와 코드 스타일

전체 TDD는 강제하지 않습니다. **결과가 명확한 핵심 로직**에는 단위 테스트를 작성합니다. — 추천 점수 계산, 예산·통근시간 조건 판별, 리뷰 수정·삭제 권한, 외부 API 응답 변환, 예외 발생 조건.

- assertion은 **AssertJ**(`assertThat`, `assertThatThrownBy`)를 씁니다.
- 한 테스트는 하나의 케이스만 검증하고, 조건이 다르면 `if`로 분기하지 말고 테스트를 나눕니다.
- private 메서드를 직접 테스트하지 않습니다. 필요하다고 느껴지면 별도 책임으로 분리할 수 있는지 검토합니다.
- 테스트 이름은 한글로 작성하고 조건과 예상 결과가 드러나게 씁니다. (`예산을_초과한_동네는_추천_후보에서_제외한다`)

**코드 스타일은 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)를 따릅니다.** 다음 두 가지만 다릅니다.

- 들여쓰기 **공백 4칸** (Google은 2칸)
- 한 줄 **최대 120자** (Google은 100자)

들여쓰기·인코딩·줄바꿈은 `.editorconfig`로 관리하므로 IDE에서 EditorConfig 지원을 켜두세요.

## 11. 보류 중인 결정

- **인증·인가 방식(세션 / JWT) 미정.** 결정 후 인증 사용자 정보를 Controller에 전달하는 방식과 401·403 응답 형식을 9장에 추가합니다.
- **컴포넌트 스캔·MapperScan 변경 미적용.** 현재 `RootConfig`는 `com.kbait.anchack.service`, `ServletConfig`는 `com.kbait.anchack.controller`만 스캔하므로 2장의 구조를 지금 적용하면 빈이 등록되지 않습니다. 별도 PR에서 처리하며, 그때 `common/exception`의 `@RestControllerAdvice` 등록 여부도 함께 확인합니다.
- **선행 작업.** Lombok·AssertJ 의존성 추가, `mybatis-config.xml` 설정, `.editorconfig` 추가.

## 12. PR 전 확인사항

공통 항목은 [CONTRIBUTING.md](https://github.com/kb-a-it/.github/blob/main/CONTRIBUTING.md) 5장을 확인하고, 여기서는 Java·Spring 항목만 봅니다.

- [ ] Controller가 Service 인터페이스에 의존하고 Mapper를 직접 호출하지 않습니다.
- [ ] `@Transactional`을 구현체에 선언했습니다.
- [ ] 요청 DTO와 응답 DTO를 구분했고 응답을 `ApiResponse`로 감쌌습니다.
- [ ] `@Data`를 쓰지 않았고, MyBatis 매핑 클래스에 `@NoArgsConstructor`가 있습니다.
- [ ] `SELECT *`와 사용자 입력값 `${}`를 쓰지 않았습니다.
- [ ] 반복문 안에서 DB를 반복 호출하지 않습니다.
- [ ] 빈 `catch`가 없고 `ErrorCode`에 정의된 예외를 사용했습니다.
- [ ] 권한과 소유권을 서버에서 검증했습니다.
- [ ] 핵심 로직의 단위 테스트를 작성했습니다.

## 13. 결정 기록

| 날짜 | 결정 | 검토한 대안 | 이유 |
|---|---|---|---|
| 2026-07 | 기능 기준 패키지 구조 | 계층 기준 구조 | 담당 기능 코드 탐색과 영향 범위 파악 |
| 2026-07 | Service 인터페이스 + 구현체 분리 | 구현체만 사용 | 병렬 작업 시 계약 우선 확정 |
| 2026-07 | 응답을 `ApiResponse<T>`로 래핑 | DTO 직접 반환 | 성공·오류 응답 형식 통일 |
| 2026-07 | Lombok 사용 | 직접 작성 | 반복 코드 감소 |
| 2026-07 | `mapUnderscoreToCamelCase` 사용 | 컬럼 별칭, resultMap | SQL 중복 제거 |
| 2026-07 | 테스트 assertion은 AssertJ | JUnit 기본 assertion | 실패 메시지 가독성 |
| — | 인증·인가 방식 | 세션 / JWT | **미정** |

> 규칙의 추가·변경·삭제 기준은 [CONTRIBUTING.md](https://github.com/kb-a-it/.github/blob/main/CONTRIBUTING.md)의 컨벤션 관리 규칙을 따릅니다.
