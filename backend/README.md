# WriteGrow Backend

서비스 소개와 API 목록은 [루트 README](../README.md) 를 참고한다. 이 문서는 **백엔드를 만지는
사람에게 필요한 것**만 다룬다 — 실행 방법, 코드 규칙, 그리고 실제로 한 번씩 걸렸던 함정들.

Spring Boot 4.1 / Java 21 / PostgreSQL 16 / Flyway / AWS S3

## 실행

```bash
docker compose -f ../docker-compose.yml up -d
./gradlew bootRun
```

Swagger UI: http://localhost:8080/swagger-ui.html

**기동 조건이 둘이다.** Postgres 가 떠 있어야 하고, S3 환경 변수가 있어야 한다.

| 변수 | 설명 |
| :--- | :--- |
| `S3_BUCKET` | 손글씨 저장 버킷 |
| `S3_ACCESS_KEY` / `S3_SECRET_KEY` | IAM 사용자 키 |

값이 없으면 기동 시 버킷 확인에서 바로 실패한다. `S3BucketVerifier` 가 `@PostConstruct` 에서
`HeadBucket` 을 한 번 호출하기 때문인데, 이게 없으면 버킷명 오타나 잘못된 자격 증명이
**아이가 손글씨를 제출하는 순간**에야 드러난다. 그때는 이미 획을 다 모은 뒤라 분석 실패로만
기록된다. IntelliJ 는 실행 구성의 Environment variables 에 넣는다.

Postgres 가 없으면 `Unable to determine Dialect without JDBC metadata` 로 실패한다. 원인을
알기 어려운 메시지지만 대부분 "Docker 를 안 켰다" 이다.

## 프로파일

| 프로파일 | 용도 | 특징 |
| :--- | :--- | :--- |
| `dev` | 로컬 개발 (기본값) | Docker Postgres, 실제 S3(`dev/` 접두사), AI 는 stub |
| `prod` | 운영 | DB·S3·AI 주소를 환경 변수로 주입 |
| `test` | 테스트 (`src/test/resources`) | datasource 는 Testcontainers 가 주입, 버킷 확인 끔 |

AI 서버 사용 여부는 프로파일이 아니라 `writegrow.ai.stub` 프로퍼티로 결정된다
(`StubAiAnalysisClient` / `AiAnalysisHttpClient` 의 `@ConditionalOnProperty`).

## 패키지 구조

최상위는 `domain` / `global` / `infra` 세 개다.

```
com.example.writegrow
├── global      config(설정), common(공통 응답/엔티티), exception(예외 처리), resolver
├── domain      account, writing, handwriting, analysis, activity, report
└── infra       s3, ai
```

도메인 패키지 내부는 항상 같은 구조를 따른다.

```
domain/<name>
├── controller
├── dto/request, dto/response
├── entity
├── exception     <Name>ErrorCode(enum implements ErrorCode), <Name>Exception
├── repository    <Name>Repository(인터페이스 하나, JpaRepository 상속)
└── service       <Name>Service(인터페이스), <Name>ServiceImpl
```

`report` 만 예외다. 여러 도메인의 기록을 읽어 요약만 하는 읽기 전용이라 자기 엔티티가 없고,
`entity` · `repository` 패키지를 두지 않는다.

## 코드 규칙

- **Service 는 인터페이스와 구현 클래스로 분리**한다(`<Name>Service` / `<Name>ServiceImpl`).
- **Repository 는 인터페이스 하나**만 두고 `JpaRepository` 를 상속한다. Spring Data 가 구현체를
  만들어 주므로 위임만 하는 클래스는 의미 없는 중복이다. 복잡한 동적 쿼리가 필요해지면 그때
  `<Name>RepositoryCustom` + `<Name>RepositoryImpl` 을 추가한다.
- **예외**는 `global.exception.ErrorCode` 를 도메인별 enum 이 구현하고,
  `<Name>Exception extends BaseException` 으로 감싼다. 응답 변환은 `GlobalExceptionHandler`
  한 곳에서만 한다. **컨트롤러에서 try-catch 하지 않는다.**
- **DTO** 는 `request` / `response` 로 나누고 record 로 작성한다. 엔티티를 컨트롤러·DTO 로
  노출하지 않는다.
- **애그리거트 경계**: 같은 애그리거트 내부는 연관관계 매핑(`@ManyToOne`), 다른 애그리거트는
  ID 참조. 예) `Writing ↔ WritingRevision` 은 연관관계, `Writing.profileId` 는 ID 참조.
- **응답은 `ApiResponse<T>` 로 통일**한다. 오류 응답만 `ErrorResponse` 를 쓴다 — 두 타입을
  나눠야 Swagger 예시가 섞이지 않는다.
- **모든 API 에 Swagger 애노테이션을 단다.** 컨트롤러 `@Tag`, 엔드포인트 `@Operation` +
  `@ApiResponses`, DTO 필드 `@Schema(description, example)`. 예시 값은 실제 아이 문장을 쓴다.
- **구현한 기능에는 단위 테스트를 함께 작성**한다. `...ServiceImpl` 은 Mockito 로, 도메인 규칙은
  엔티티 테스트로 검증한다.
- 스키마 변경은 항상 Flyway 마이그레이션(`src/main/resources/db/migration`)으로 한다.
  `ddl-auto` 는 `validate` 다.

### 페이지네이션

**"계속 쌓이는 목록"에만 적용한다.** 기준은 개수가 시간에 따라 무한히 늘어나는가다.

- 적용: `GET /writings`(매일 쌓임). 기본 20건, `createdAt,id` 내림차순. 정렬이 흔들리면 페이지
  경계에서 항목이 중복·누락되므로 **동점 대비 보조 정렬 기준(`id`)을 반드시 포함**한다.
- 미적용: 계정의 프로필 목록(계정당 몇 개), 글의 수정 이력(글당 2~3건), 오류 유형별 프로필(6종
  고정). 이런 곳에 페이지를 두면 클라이언트 코드만 복잡해진다.
- 페이지 응답은 `PageResponse<T>` 를 쓰고 Spring Data 의 `Page` 를 그대로 내보내지 않는다.
- 리포지토리 메서드 이름에 `OrderBy` 를 넣는 것은 **페이지 없는 조회에서만** 한다. `Pageable` 을
  받는 메서드에 `OrderBy` 를 함께 쓰면 정렬 절이 중복되고 클라이언트의 `sort` 가 무시된다.

## 도메인 흐름

### 키보드 글

```
POST /writings → PATCH /writings/{id}(임시 저장) → POST /writings/{id}/submit
  → 즉시 CONFIRMED → 오류 분석 시작
```

### 손글씨 글

```
POST /writings                        글 시작 (inputType=PEN)
POST /writings/{id}/strokes           작성 중 획 데이터 전송 (여러 번, 멱등)
POST /writings/{id}/handwriting-image 렌더 이미지 업로드
POST /writings/{id}/submit            제출 → 202, OCR 분석 시작
GET  /writings/{id}/analysis          폴링 (PROCESSING → SUCCEEDED/FAILED)
PATCH /writings/{id}/text             아이가 변환 텍스트 확인·수정 → CONFIRMED → 오류 분석 시작
POST /writings/{id}/rewrite           마음에 안 들면 DRAFT 로 되돌리기
POST /writings/{id}/analysis/retry    실패 시 재시도 (원본 재업로드 불필요)
```

**분석은 두 번 돈다.** 손글씨 OCR(REQ-02)과 오류 분석(REQ-03)은 시점도 입력도 다르다.
둘 다 이벤트로 시작해서 글쓰기 도메인이 분석 도메인을 직접 의존하지 않게 한다.

| 이벤트 | 발행 시점 | 하는 일 |
| :--- | :--- | :--- |
| `HandwritingSubmittedEvent` | 손글씨 제출 | OCR + 작성 과정 지표 |
| `TextConfirmedEvent` | 최종본 확정 (키보드 제출 / 텍스트 확인) | 오류 후보 분석 |

둘 다 `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` 로 시작한다. 커밋 이후에 시작하므로
분석 스레드가 아직 저장되지 않은 글을 읽는 일이 없다.

**오래 걸리는 AI 호출은 트랜잭션 밖에 둔다.** 상태 변경만 `REQUIRES_NEW` 짧은 트랜잭션으로
처리한다(`AnalysisResultService`, `ErrorAnalysisResultService`). 60초짜리 호출이 커넥션을 물고
있으면 안 된다.

## 지켜야 할 도메인 규칙

- **낮은 확신도 후보는 아이에게 오류로 확정해 보여주지 않는다.** 임계값 미만이면 아이 응답에서
  빠지고, 반복 오류 프로필 집계에도 반영하지 않으며, 보호자·교사 검토 대상으로만 분리한다.
  임계값은 둘로 나뉜다 — `writegrow.ocr.confidence-threshold`(0.7, "글자를 잘못 읽었을 가능성")와
  `writegrow.error-analysis.confidence-threshold`(0.75, "교정이 필요한가"). 성격이 달라 한 값으로
  묶지 않는다.
- **분석이 실패해도 손글씨 원본과 획 데이터는 절대 삭제하지 않는다.** 재시도의 근거이자 명세상
  요구사항이다. 다시 쓰기도 획을 지우지 않고 `attemptNo` 로 시도를 구분한다.
- **획 데이터는 결과 이미지만큼 중요하다.** 이 서비스의 차별점이 "결과가 아니라 과정"이므로,
  획의 순서와 시각 정보를 잃는 최적화(예: 좌표만 저장)를 하지 않는다.
- **보호자 리포트에 전문적인 진단을 넣지 않는다.** "다음 집중 영역"은 유형과 근거 코드만
  내려주고 문구는 화면이 정한다.
- 빈 글은 제출할 수 없다.

## 인증에 대한 임시 규칙

MVP 는 로그인·회원가입을 구현하지 않는다. **`X-Profile-Id` 헤더**로 요청 주체를 식별하며,
컨트롤러는 `@CurrentProfile Long profileId` 로 주입받는다.

**Spring Security 의존성도 두지 않는다.** 모든 요청이 permitAll 이면 필터 체인이 하는 일이 없는데,
설정 클래스만 남아 있으면 "보안이 걸려 있다"는 착각을 준다. CORS 는 `WebConfig#addCorsMappings`
가 담당한다.

**인증이 없어도 소유권 규칙은 지금부터 지킨다.**

- 본인 글만 접근: `writing.validateOwner(profileId)`
- 보호자는 같은 계정의 아이만 열람: `ProfileService#getViewableChild`
- **아이는 형제자매의 기록도 볼 수 없다.** 계정만 확인하면 남매가 서로의 오류 분석을 들여다볼
  수 있어 조회자가 아동이면 본인 기록만 허용한다.

인증을 붙일 때는 `spring-boot-starter-security` 를 추가하고 `SecurityFilterChain` 을 만든 뒤
`CurrentProfileArgumentResolver` 가 헤더 대신 토큰에서 프로필 ID 를 꺼내게 바꾸면 된다.
CORS 설정은 그때 SecurityFilterChain 쪽으로 옮긴다. **컨트롤러와 서비스는 손대지 않는다.**

## S3 저장소

손글씨 렌더 이미지와 병합된 획 문서만 올라간다. AI 서버에는 객체가 아니라 presigned GET
URL(기본 10분)만 넘기므로 버킷은 **퍼블릭 액세스를 모두 차단**한다.

**dev 와 prod 가 버킷 하나를 접두사로 나눠 쓴다.** LocalStack 은 쓰지 않는다 — 로컬에서 통과한
것이 실제 AWS 에서도 통과한다는 근거가 되어야 하는데, 에뮬레이터는 그 근거가 못 된다.

```
{key-prefix}handwriting/{yyyy}/{MM}/{dd}/{writingId}/image-{uuid}.{ext}
{key-prefix}handwriting/{yyyy}/{MM}/{dd}/{writingId}/strokes-{uuid}.json
```

키를 만드는 곳은 `StorageKeyFactory` 하나뿐이고 접두사도 거기서만 붙인다.
`writegrow.s3.key-prefix` 는 dev 가 `dev/`, prod 가 `prod/`, 테스트가 `test/` 다.
**이 값이 겹치면 로컬에서 운영 데이터를 건드리게 되므로** 바꿀 때 주의한다.

- **버킷 정책도 CORS 도 필요 없다.** 같은 계정의 IAM 사용자 권한은 IAM 정책 하나로 끝나고,
  브라우저가 S3 에 직접 요청하는 경로가 없다. 프론트가 `fetch()` 로 이미지를 읽거나 브라우저
  직접 업로드로 바뀌면 그때 CORS 를 추가한다.
- **IAM 최소 권한**: `s3:PutObject` · `s3:GetObject` 를 `{버킷}/*/handwriting/*` 에,
  `s3:ListBucket` 을 버킷 자체에. `ListBucket` 은 기동 시 `HeadBucket` 에 필요하다.
- **수명주기 만료 규칙을 두지 않는다.** 위 삭제 금지 규칙과 정면으로 충돌한다.

## 함정 모음

### AI 서버 호출은 HTTP/1.1 로 고정한다

자바 `HttpClient` 는 기본이 HTTP/2 라, 평문 `http://` 로 보낼 때 `Upgrade: h2c` 로 승격을
시도한다. AI 서버(uvicorn)는 h2c 를 지원하지 않아 승격이 거부되고, **그 과정에서 요청 본문이
전달되지 않아 422 가 돌아온다.** `RestClientConfig` 가 `HTTP_1_1` 로 고정한다.

stub 으로는 드러나지 않는다. 같은 JVM 안의 빈이라 HTTP 를 타지 않기 때문이다. **AI 를 붙일
때는 로컬에서 `writegrow.ai.stub=false` 로 한 번 확인하고 배포한다.**

### Spring Boot 4

Boot 3 습관대로 하면 걸리는 것들이다. 실제로 기동 중에 하나씩 터졌던 항목이다.

- **자동설정이 모듈별로 쪼개졌다.** 라이브러리만 클래스패스에 올리면 자동설정이 붙지 않는다.
  - Flyway: `flyway-core` 만 넣으면 마이그레이션이 **조용히** 실행되지 않는다 →
    `spring-boot-starter-flyway` 필요
  - RestClient: `spring-boot-starter-webmvc` 에 `RestClient.Builder` 빈이 없다 →
    `spring-boot-starter-restclient` 필요
  - 새 기술을 붙일 때는 `spring-boot-starter-<name>` 이 있는지 먼저 확인한다.
- **Jackson 3 를 쓴다.** 자동 설정되는 빈은 `tools.jackson.databind.ObjectMapper` 이고,
  Jackson 2 의 `com.fasterxml.jackson.databind.ObjectMapper` 빈은 존재하지 않는다.
  - 애노테이션(`@JsonProperty` 등)은 여전히 `com.fasterxml.jackson.annotation` 패키지다.
  - 예외는 `tools.jackson.core.JacksonException` 이고 **unchecked** 다.
  - Hibernate 7.4 는 `Jackson3JsonFormatMapper` 를 내장해 jsonb 매핑은 그대로 동작한다.
- **Spring Data 4 에서 일부 클래스의 패키지가 바뀌었다.** 예: `PropertyReferenceException` 이
  `org.springframework.data.mapping` → `org.springframework.data.core` 로 이동.
  `cannot find symbol` 이 뜨면 없어진 게 아니라 옮겨진 경우가 많다.
- **Testcontainers 2.x** 를 관리한다. 아티팩트명이 `testcontainers-*` 접두사로 바뀌었고
  (`org.testcontainers:postgresql` → `org.testcontainers:testcontainers-postgresql`),
  `PostgreSQLContainer` 는 `org.testcontainers.postgresql` 패키지의 **비제네릭** 클래스다.
- 테스트 스타터도 모듈별로 나뉜다: `spring-boot-starter-data-jpa-test`, `-webmvc-test`.

### Windows 에서 개발할 때

- **셸 스크립트나 실행 파일을 추가하면 실행 비트를 직접 넣어야 한다.** git 이 Windows
  파일시스템의 권한을 읽지 못해 `100644` 로 기록되고, Linux CI 러너에서 `Permission denied`
  (exit 126) 로 빌드가 죽는다. `.gitattributes` 로는 막을 수 없다.

  ```bash
  git update-index --chmod=+x <파일>
  ```

- **컨테이너에서 실행되는 파일은 LF 로 고정한다.** 루트 `.gitattributes` 가 `*.sh`, `*.yml` 을
  LF 로 묶어 둔다. CRLF 로 체크아웃되면 `#!/bin/bash\r` 가 되어 스크립트가 실행되지 않는다.
- **JSON 본문에 한글이 있으면 curl 에 인자로 넘기지 않는다.** Git Bash 가 argv 를 재인코딩해
  깨뜨리고, Jackson 파싱이 실패해 `INVALID_REQUEST` 가 돌아온다. 파일이나 표준입력을 쓴다.
  파일 업로드(`-F`)도 절대경로에 `;type=` 을 붙이면 MSYS2 가 경로를 망가뜨리므로, 디렉터리를
  옮기고 상대 파일명으로 넘긴다.

## 테스트

```bash
./gradlew test
```

`WritegrowApplicationTests` 는 Testcontainers 로 Postgres 를 띄워 Flyway 마이그레이션과 엔티티
매핑을 검증한다. Docker 가 없으면 통째로 건너뛰므로 단위 테스트만 돌릴 때 빌드가 깨지지 않는다.

**CI 에는 S3 자격 증명을 넣지 않는다.** 테스트는 S3 에 붙지 않고(`verify-on-startup: false`,
`S3Client` 는 목), 넣으면 쓰이지도 않으면서 유출면만 넓어진다.
