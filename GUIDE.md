# 안착(anchack) 백엔드 로컬 실행 가이드

`git pull` 이후 로컬에서 실행하기 위한 가이드입니다. Docker Compose로 MySQL + Flyway 마이그레이션 + 앱(WAR on Tomcat)을 한 번에 띄웁니다.

## 사전 준비물

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) 설치 및 **실행 중**이어야 함
- (IDE에서 직접 실행할 경우) JDK 17

## 1. Docker Desktop이 켜져 있는지 확인

Docker Desktop 앱이 실행 중이 아니면 `docker compose` 명령이 아래처럼 실패합니다.

```
unable to get image 'flyway/flyway:10': ... open //./pipe/dockerDesktopLinuxEngine: The system cannot find the file specified.
```

아래 명령으로 데몬이 응답하는지 먼저 확인하세요.

```bash
docker info
```

에러가 나면 Docker Desktop을 실행하고 트레이 아이콘이 "Running" 상태가 될 때까지 기다린 뒤 다시 시도하세요.

## 2. 3307 포트가 비어 있는지 확인

로컬 MySQL과의 충돌을 피하려고 `mysql` 컨테이너의 호스트 포트를 **3307**로 매핑해뒀습니다 (컨테이너 내부/컨테이너 간 통신은 그대로 3306 사용 — `flyway`, `app`이 `mysql:3306`으로 접속하는 설정은 안 바뀜). 그래도 이미 3307을 쓰는 다른 프로세스가 있으면 아래 에러가 납니다.

```
Error response from daemon: ports are not available: exposing port TCP 0.0.0.0:3307 -> 127.0.0.1:0: ...
```

**Windows (PowerShell / Git Bash)**

```bash
netstat -ano | grep ':3307'
```

가장 오른쪽 열이 PID입니다. 작업 관리자에서 해당 PID를 찾아 종료하거나, 해당 서비스를 중지하세요.

```powershell
Get-Process -Id <PID>
Stop-Process -Id <PID>
```

**macOS / Linux**

```bash
lsof -i :3307
```

> 호스트에서 DB 클라이언트(DBeaver, MySQL Workbench 등)로 이 도커 MySQL에 직접 접속하고 싶다면 `127.0.0.1:3307`로 접속하면 됩니다 (3306이 아님).

## 3. 환경변수 파일 준비

`.env` 파일은 커밋되지 않으므로 직접 만들어야 합니다. `.env.example`을 복사하세요.

```bash
cp .env.example .env
```

기본값(`DB_ROOT_PASSWORD=localpassword`) 그대로 써도 되고, 원하면 값을 바꿔도 됩니다.

**`JWT_SECRET`은 필수입니다.** 로그인 시 JWT를 발급하는데, 이 값이 없으면(또는 32바이트/256bit 미만이면) 로그인하는 순간 서버가 에러를 던집니다. 아래 명령으로 각자 생성해서 채우세요 (팀원마다 값이 달라도 상관없음 — 로컬 개발용).

```bash
openssl rand -base64 32
```

나온 값을 `.env`의 `JWT_SECRET=`에 붙여넣으면 됩니다.

카카오 로그인까지 테스트하려면 `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `KAKAO_REDIRECT_URI`도 채워야 합니다(카카오 개발자 콘솔에서 발급). 비워두면 앱은 정상 기동되지만 카카오 로그인만 실패합니다.

## 4. 전체 스택 실행

```bash
docker compose up --build
```

정상적으로 뜨면 아래 순서로 진행됩니다.

1. `mysql` 컨테이너가 healthy 상태가 될 때까지 대기
2. `flyway` 컨테이너가 `src/main/resources/db/migration`의 마이그레이션을 적용하고 종료 (`Successfully applied ... migrations` 로그가 보이면 성공)
3. `app` 컨테이너(Tomcat 9 + WAR)가 8080 포트로 기동

백그라운드로 띄우고 싶으면 `-d` 옵션을 추가하세요.

```bash
docker compose up --build -d
```

## 5. 정상 기동 확인

```bash
curl http://localhost:8080/api/health
```

`OK`가 나오면 정상입니다.

마이그레이션이 잘 적용됐는지 직접 보고 싶으면:

```bash
docker compose logs flyway
```

## 6. 자주 겪는 문제

| 증상 | 원인 / 해결 |
|---|---|
| `docker compose up`이 데몬 연결 에러로 즉시 실패 | Docker Desktop 미실행 → 1번 항목 참고 |
| `mysql` 컨테이너가 포트 바인딩 실패로 못 뜸 | 로컬 3307 포트 점유 → 2번 항목 참고 |
| `flyway`가 `RSA public key is not available` 에러로 실패 | MySQL 8의 `caching_sha2_password` 인증 문제. `docker-compose.yml`의 JDBC URL에 이미 `allowPublicKeyRetrieval=true&useSSL=false`가 반영되어 있어야 함 (반영 안 된 옛 버전이라면 최신 `docker-compose.yml`로 다시 pull) |
| 스키마를 처음부터 다시 적용하고 싶음 | `docker compose down -v` 로 볼륨까지 삭제 후 `docker compose up --build` 재실행 (로컬 더미 데이터가 전부 날아가니 주의) |
| 코드만 바꿨는데 반영이 안 됨 | `docker compose up --build` 로 이미지 재빌드 필요 (`--build` 없이 `up`만 하면 기존 이미지를 재사용함) |
| 앱은 잘 뜨는데 카카오 로그인(`/api/auth/kakao/callback`)만 안 됨 | `.env`에 `KAKAO_CLIENT_ID` / `KAKAO_CLIENT_SECRET` / `KAKAO_REDIRECT_URI`가 비어 있으면 앱 자체는 조용히 잘 뜨지만 카카오 로그인만 실패함. `.env`에 카카오 개발자 콘솔에서 발급받은 값을 채우고 `docker compose up --build`로 재기동하면 해결됨 (3번 항목 참고) |
| 로그인 시도하면 `JWT_SECRET 환경변수가 설정되지 않았습니다` 또는 `JWT_SECRET은 최소 32바이트...` 에러 | `.env`의 `JWT_SECRET`이 비어 있거나 32바이트 미만. `openssl rand -base64 32`로 생성해서 채우고 재기동 (3번 항목 참고) |
| `flyway`가 `Validate failed: Migration checksum mismatch for migration version 1` 에러로 실패 | 예전에 이 프로젝트를 이미 한 번 세팅해서 V1 마이그레이션을 적용한 적이 있는데, 그 뒤 `V1__init_schema.sql` 내용이 수정된 경우(파일명은 같아도 체크섬이 달라짐). `docker compose down -v` 로 로컬 DB 볼륨을 통째로 지우고 `docker compose up --build`로 처음부터 다시 적용하면 해결됨 |
| `.env`에 `RECOMMENDATION_REASON_MODE=openai`로 바꿨는데 `recommendationReason`/`caution`이 계속 "추후 openai api 호출" 고정 문구로 나옴 | `OPENAI_PROXY_TOKEN`이 비어 있으면 OpenAI 프록시 호출이 조용히 실패하고 플레이스홀더로 폴백함(에러가 눈에 안 보임 — 전체 추천 요청 자체는 정상 200 응답). 프록시 대시보드에서 발급받은 팀 토큰을 `.env`의 `OPENAI_PROXY_TOKEN`에 채우고 재기동 |

## 7. 스택 종료

```bash
docker compose down       # 컨테이너만 정리 (DB 데이터는 volume에 남음)
docker compose down -v    # 컨테이너 + DB 데이터까지 완전 초기화
```

## 8. IDE(IntelliJ 등)에서 앱만 직접 실행하고 싶은 경우

Docker 없이 앱만 로컬 JVM으로 띄우려면:

1. 로컬에 MySQL 8이 떠 있어야 하고, `anchack` 데이터베이스가 있어야 합니다.
2. `src/main/resources/application.properties`를 직접 만들어야 합니다(git에는 없음 — `.gitignore`로 제외됨).

```properties
jdbc.driver=com.mysql.cj.jdbc.Driver
jdbc.url=jdbc:mysql://127.0.0.1:3306/anchack
jdbc.username=root
jdbc.password=<로컬 MySQL 비밀번호>

# 필수 — 없으면 로그인 시 에러 발생 (openssl rand -base64 32 로 생성)
jwt.secret=<32바이트 이상 랜덤 문자열>
jwt.expiration-ms=3600000

# 카카오 로그인을 테스트할 경우에만 필요 (없어도 앱 자체는 뜸)
kakao.client-id=<카카오 개발자 콘솔 REST API 키>
kakao.client-secret=
kakao.redirect-uri=<카카오 개발자 콘솔에 등록한 Redirect URI>
```

## 9. 국토부 전월세 최근 12개월 일회성 초기 적재

이 기능은 Tomcat 기동, HTTP 요청, 주간 스케줄러와 별개인 Gradle CLI다. 반드시 명시적인
Gradle 명령으로만 실행되며, 전체 적재는 `--all` 옵션 없이는 시작되지 않는다.

### 9-1. 환경변수 준비

Gradle은 `.env`를 자동으로 읽지 않는다. Docker Compose의 MySQL을 호스트 CLI에서 사용할 때는
아래처럼 `.env`를 export하고 Spring JDBC 환경변수를 준비한다. API 키와 DB 비밀번호를 Gradle
옵션이나 명령 문자열에 직접 쓰지 않는다.

```bash
set -a
source .env
set +a

export JDBC_URL='jdbc:mysql://127.0.0.1:3307/anchack?serverTimezone=Asia/Seoul&characterEncoding=UTF-8&useUnicode=true&allowPublicKeyRetrieval=true&useSSL=false'
export JDBC_USERNAME='root'
export JDBC_PASSWORD="$DB_ROOT_PASSWORD"
```

필수 환경변수는 `MOLIT_API_SERVICE_KEY`, `KAKAO_ROUTE_REST_API_KEY`, `JDBC_URL`,
`JDBC_USERNAME`, `JDBC_PASSWORD`다. 카카오 주소 검색은 전월세 지번을 좌표로 변환할 때 사용하며,
키가 없거나 인증·네트워크·서버 오류가 발생하면 해당 구·월의 기존 거래를 교체하지 않는다.
`MOLIT_API_NUM_OF_ROWS`, `MOLIT_API_CONNECT_TIMEOUT_MS`, `MOLIT_API_READ_TIMEOUT_MS`는 기존 기본값을
그대로 사용하거나 필요할 때만 설정한다.

전체 적재 중에는 별도로 실행 중인 Tomcat의 국토부 주간 스케줄러를 중지하거나 비활성화해야 한다.
CLI 자신의 Context에서는 국토부·카카오 장소·CCTV cron을 모두 비활성화하지만, 다른 프로세스의
스케줄러까지 막을 수는 없다.

### 9-2. 1개 구·1개월 검증

전체 `--all` 실행 전에 카카오 주소 검색 호출량과 쿼터를 반드시 확인한다. 최근 12개월 × 서울 25개
구의 고유 `구 코드|법정동명|지번` 주소 수를 호출량의 상한으로 산정하고, 카카오 개발자 콘솔에서
일·월 쿼터와 429 재시도 정책(500ms 대기 후 1회)이 전체 적재 규모를 감당하는지 확인한다. 확인 없이
전체 초기 적재를 시작하지 않는다.

현재가 2026년 8월이면 완료된 직전월인 관악구(`11620`) 2026년 7월을 먼저 검증한다.

```bash
./gradlew molitRentInitialLoad \
  --args='--year-month=2026-07 --lawd-code=11620'
```

부분 실행의 월은 KST 기준 현재월과 직전 11개월 안에 있어야 하며, 구 코드는 서울 25개 법정
시군구 코드 중 하나여야 한다. 옵션 오류는 Spring Context, DB, 외부 API에 접근하기 전에 종료된다.
부분 실행 후 카카오 호출 건수, 지번 없음·주소 검색 실패·경계 판별 실패·행정동 DB 조회 실패 건수와
429 발생 여부를 확인한 뒤에만 `--all` 실행으로 넘어간다.

실행 직후 같은 명령을 한 번 더 실행하고 아래 SQL의 건수가 누적되지 않는지 확인한다. 기존 적재는
`gu_code`와 거래월 범위를 삭제한 뒤 다시 INSERT하는 정책이다. 즉, 거래 ID는 바뀔 수 있어도
월·구 범위의 행 수가 누적되면 안 된다.

```sql
SELECT
    gu_code,
    DATE_FORMAT(transaction_date, '%Y-%m') AS deal_month,
    house_type,
    COUNT(*) AS transaction_count
FROM rental_transactions
WHERE gu_code = '11620'
  AND transaction_date >= '2026-07-01'
  AND transaction_date < '2026-08-01'
GROUP BY gu_code, DATE_FORMAT(transaction_date, '%Y-%m'), house_type
ORDER BY house_type;
```

### 9-3. 전체 실행과 실패 재실행

전체 실행은 최근 12개월을 오래된 월부터 처리하며, 각 월 안에서 서울 25개 구를 순차 처리한다.

```bash
./gradlew molitRentInitialLoad --args='--all'
```

한 월·구가 실패해도 나머지 작업은 계속한다. 종료 시 성공·실패 수와 실패한 대상, 예외 유형,
복사 가능한 부분 재실행 명령을 출력한다. 실패가 하나라도 있으면 프로세스와 Gradle task는 exit code
`1`로 종료한다. 로그에 나온 재실행 명령을 그대로 사용한다.

```bash
./gradlew molitRentInitialLoad \
  --args='--year-month=2026-07 --lawd-code=11620'
```

전체 성공은 exit code `0`, 입력 오류 또는 Context 초기화 실패는 `2`다.

### 9-4. 전체 적재 후 확인

아래 SQL은 최근 12개월 × 25개 구의 월·구 조합 중 행이 없거나 API 유형별 건수가 0인 대상을
찾기 위한 점검용이다. `0건`이 항상 오류라는 뜻은 아니므로 국토부 API의 실제 응답과 함께 확인한다.

```sql
WITH RECURSIVE target_months AS (
    SELECT DATE_FORMAT(DATE_SUB(DATE(CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+09:00')), INTERVAL 11 MONTH), '%Y-%m-01') AS month_start
    UNION ALL
    SELECT DATE_ADD(month_start, INTERVAL 1 MONTH)
    FROM target_months
    WHERE month_start < DATE_FORMAT(DATE(CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+09:00')), '%Y-%m-01')
),
target_gus AS (
    SELECT '11110' AS gu_code UNION ALL SELECT '11140' UNION ALL SELECT '11170'
    UNION ALL SELECT '11200' UNION ALL SELECT '11215' UNION ALL SELECT '11230'
    UNION ALL SELECT '11260' UNION ALL SELECT '11290' UNION ALL SELECT '11305'
    UNION ALL SELECT '11320' UNION ALL SELECT '11350' UNION ALL SELECT '11380'
    UNION ALL SELECT '11410' UNION ALL SELECT '11440' UNION ALL SELECT '11470'
    UNION ALL SELECT '11500' UNION ALL SELECT '11530' UNION ALL SELECT '11545'
    UNION ALL SELECT '11560' UNION ALL SELECT '11590' UNION ALL SELECT '11620'
    UNION ALL SELECT '11650' UNION ALL SELECT '11680' UNION ALL SELECT '11710'
    UNION ALL SELECT '11740'
),
monthly_counts AS (
    SELECT
        gu_code,
        DATE_FORMAT(transaction_date, '%Y-%m-01') AS month_start,
        SUM(house_type = '오피스텔') AS officetel_count,
        SUM(house_type IN ('연립', '다세대', '연립다세대')) AS row_house_count,
        SUM(house_type IN ('단독', '다가구')) AS single_house_count,
        COUNT(*) AS total_count
    FROM rental_transactions
    WHERE transaction_date >= DATE_FORMAT(DATE_SUB(DATE(CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+09:00')), INTERVAL 11 MONTH), '%Y-%m-01')
      AND transaction_date < DATE_ADD(DATE_FORMAT(DATE(CONVERT_TZ(UTC_TIMESTAMP(), '+00:00', '+09:00')), '%Y-%m-01'), INTERVAL 1 MONTH)
    GROUP BY gu_code, DATE_FORMAT(transaction_date, '%Y-%m-01')
)
SELECT
    target_gus.gu_code,
    target_months.month_start,
    COALESCE(monthly_counts.total_count, 0) AS total_count,
    COALESCE(monthly_counts.officetel_count, 0) AS officetel_count,
    COALESCE(monthly_counts.row_house_count, 0) AS row_house_count,
    COALESCE(monthly_counts.single_house_count, 0) AS single_house_count
FROM target_months
CROSS JOIN target_gus
LEFT JOIN monthly_counts
    ON monthly_counts.gu_code = target_gus.gu_code
   AND monthly_counts.month_start = DATE_FORMAT(target_months.month_start, '%Y-%m-01')
WHERE COALESCE(monthly_counts.total_count, 0) = 0
   OR COALESCE(monthly_counts.officetel_count, 0) = 0
   OR COALESCE(monthly_counts.row_house_count, 0) = 0
   OR COALESCE(monthly_counts.single_house_count, 0) = 0
ORDER BY target_months.month_start, target_gus.gu_code;
```

오피스텔과 연립·다세대는 지번 주소를 카카오 좌표로 변환하고 서울 행정동 GeoJSON 경계로 판별하여
`admin_dong_id`를 저장한다. 주소 검색 결과가 없거나 경계·DB 코드가 없으면 해당 거래만 `NULL`로
저장한다. 현재 국토부 단독·다가구 응답에는 지번이 없어 정확한 행정동을 결정할 수 없으므로 거래는
버리지 않고 `admin_dong_id = NULL`로 저장한다. 법정동명과 행정동명을 직접 맞추거나 임의 분배하지
않는다. `property_metrics`는 `admin_dong_id`가 매핑된 전체 거래만 그룹별로 집계하며,
관리자 재계산 요청 시 기존 내용을 삭제하고 단일 스냅샷으로 다시 만든다.

3. `RootConfig`가 기동 시 `src/main/resources/db/migration`의 Flyway 마이그레이션을 자동 실행하므로 별도 스키마 적용 작업은 필요 없습니다.
