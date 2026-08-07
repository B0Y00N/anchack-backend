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

3. `RootConfig`가 기동 시 `src/main/resources/db/migration`의 Flyway 마이그레이션을 자동 실행하므로 별도 스키마 적용 작업은 필요 없습니다.
