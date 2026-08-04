# Backend 코드 컨벤션

안착 Backend Repository에 적용하는 규칙입니다.

브랜치·커밋·PR·보안 등 협업 규칙은 조직의 [CONTRIBUTING.md](https://github.com/kb-a-it/.github/blob/main/CONTRIBUTING.md)를 따릅니다.

---

## 1. 스택과 제약

- **사용 기술**: Java 17, Spring Framework 5.3.37(Legacy), MyBatis 3.5.16, Gradle

AI 도구로 코드를 생성하면 Spring Boot 기준 코드가 섞여 들어와 컴파일이 깨지는 경우가 많으니, 다음은 이 프로젝트에서 쓰지 않습니다.

- `@SpringBootApplication`, `@SpringBootTest`, `spring-boot-starter-*`
- `application.yml` (설정은 `application.properties`)
- `jakarta.validation`, `jakarta.servlet` 등 `jakarta.*` 패키지 (`javax.servlet`, `javax.validation` 사용)
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

> 컴포넌트 스캔과 MapperScan 변경 전까지는 기존 패키지 구조로 작업합니다. (12장 참고)

## 3. 계층 규칙

- **모든 Service는 인터페이스와 구현체를 분리합니다.** 구현체는 `service.impl`에 두고 이름은 `기능명ServiceImpl`로 작성하며, Controller는 인터페이스에 의존합니다. — 여러 명이 동시에 작업할 때 인터페이스를 계약으로 먼저 확정하기 위함입니다.
- `@Service`와 `@Transactional`은 **구현체**에 선언합니다. — 인터페이스에 선언하면 프록시 생성 방식에 따라 적용되지 않을 수 있습니다.
- 외부 API(주소·지도·경로) 호출은 `client` 패키지로 분리합니다. 외부 응답 DTO를 내부 응답으로 그대로 쓰지 않고, 외부 예외를 내부 예외로 변환합니다. — 외부 API 변경이 내부 로직으로 번지지 않도록 하기 위함입니다.

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

**Boolean 반환 접두사**

| 접두사 | 의미 |
|---|---|
| `is` | 상태나 조건이 맞는지 판단 |
| `has` | 무언가를 가지고 있는지 확인 |
| `can` | 할 수 있는지 확인 |

값을 꺼내서 바깥에서 판단하지 말고, 이 접두사로 객체에 직접 물어봅니다. (6장 참고)

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

- `@Data`는 쓰지 않습니다. `@Setter`·`@EqualsAndHashCode`·`@ToString`이 한 번에 생성되어 의도치 않은 상태 변경, 연관 객체 순환 참조로 이어집니다.
- 의존성 주입은 `@RequiredArgsConstructor` + `private final`을 사용합니다. 필드 주입(`@Autowired`)을 쓰지 않습니다.

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

| Method | URI | 설명 |
|---|---|---|
| GET | `/api/reviews` | 목록 조회 |
| GET | `/api/reviews/{reviewId}` | 단건 조회 |
| POST | `/api/reviews` | 생성 |
| PATCH | `/api/reviews/{reviewId}` | 수정 |
| DELETE | `/api/reviews/{reviewId}` | 삭제 |

**모든 응답은 `ApiResponse<T>`로 감쌉니다.** HTTP 상태 코드도 그대로 사용합니다.

```json
{ "success": true,  "data": { "reviewId": 1 }, "error": null }
{ "success": false, "data": null, "error": { "code": "REVIEW_NOT_FOUND", "message": "리뷰를 찾을 수 없습니다." } }
```

- `error.code`는 `{도메인}_{사유}` 형식의 대문자 스네이크로 작성하고, **`common/exception/ErrorCode` enum 한 곳에서만 정의합니다.** 문자열 리터럴을 직접 쓰지 않습니다.
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

## 10. 메서드 설계

- 메서드는 한 가지 일만 담당합니다. 이름에 "그리고"가 필요하다면 분리 신호입니다.
- 메서드는 **15줄 내외**를 기준으로 작성합니다. 절대 기준은 아니지만, 넘어간다면 여러 책임이 섞여 있다는 신호로 보고 메서드·클래스 분리를 검토합니다.

## 11. 테스트와 코드 스타일

전체 TDD는 강제하지 않습니다. **결과가 명확한 핵심 로직**에는 단위 테스트를 작성합니다. — 추천 점수 계산, 예산·통근시간 조건 판별, 리뷰 수정·삭제 권한, 외부 API 응답 변환, 예외 발생 조건.

- assertion은 **AssertJ**(`assertThat`, `assertThatThrownBy`)를 씁니다.
- 한 테스트는 하나의 케이스만 검증하고, 조건이 다르면 `if`로 분기하지 말고 테스트를 나눕니다.
- **성공 케이스와 실패 케이스를 함께 작성합니다.** 실패 케이스는 예외가 발생하는 조건, 경계값, 입력이 없는 경우를 확인합니다. 성공 경로만 확인하면 예외 처리 로직이 검증되지 않습니다.
- private 메서드를 직접 테스트하지 않습니다. 필요하다고 느껴지면 별도 책임으로 분리할 수 있는지 검토합니다.
- 테스트 이름은 한글로 작성하고 조건과 예상 결과가 드러나게 씁니다.

```java
@Test
void 예산을_초과한_동네는_추천_후보에서_제외한다() {
    RecommendationCondition condition = RecommendationCondition.of(500_000, 30);

    List<AdminDong> result = recommendationService.recommend(condition);

    assertThat(result).allMatch(dong -> dong.getDeposit() <= condition.getBudget());
}

@Test
void 존재하지_않는_리뷰를_수정하면_예외가_발생한다() {
    ReviewUpdateRequest request = new ReviewUpdateRequest("내용");

    assertThatThrownBy(() -> reviewService.update(999L, request))
            .isInstanceOf(ReviewNotFoundException.class);
}
```

**코드 스타일은 [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)를 따릅니다.** 다음은 예외입니다.

- **Java 코드 들여쓰기**: 공백 **4칸** (Google은 2칸)
- **한 줄 최대 120자** (Google은 100자)
- **XML(Mapper·설정 파일 등) 들여쓰기는 Java와 다르게 공백 2칸**을 씁니다. — MyBatis 공식 예제 관례를 따르고, `<where>`·`<if>`처럼 태그 중첩이 깊어지는 SQL 매퍼 특성상 4칸이면 가독성이 떨어지기 때문입니다. **Java는 4칸, XML은 2칸으로 다르다는 점에 주의하세요.**

들여쓰기·인코딩·줄바꿈은 `.editorconfig`로 관리하므로 IDE에서 EditorConfig 지원을 켜두세요. Windows·Mac 혼용 환경에서 줄바꿈 문자(LF/CRLF)가 깨지는 것을 막기 위해 `.gitattributes`도 함께 사용합니다.

## 12. 보류 중인 결정

## 13. 결정 기록

논의 과정에서 대안을 검토하고 확정한 것만 기록합니다. 미정인 항목은 12장에 둡니다.

| 날짜 | 결정 | 검토한 대안 | 이유 |
|---|---|---|---|

> 날짜는 실제 논의·병합 시점으로 채워주세요. 규칙의 추가·변경·삭제 기준은 [CONTRIBUTING.md](https://github.com/kb-a-it/.github/blob/main/CONTRIBUTING.md)의 컨벤션 관리 규칙을 따릅니다.
