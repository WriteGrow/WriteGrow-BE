# WriteGrow

초등 저학년 아동의 자유 글쓰기를 돕고, AI 가 **결과가 아니라 과정**을 분석해 성장을 보여주는 서비스.

아이는 매일 한두 문장을 키보드나 펜으로 쓴다. 손글씨는 AI 가 글자로 옮겨 주고, 아이가 직접
확인·수정해 최종본을 확정한다. 그 과정에서 어떤 오류를 반복하는지, 어디에서 머뭇거렸는지가
쌓여 보호자의 주간 리포트가 된다.

## 이 서비스가 지키는 것

기능을 더하는 것보다 아래 규칙을 지키는 쪽을 우선한다. 대상이 초등 저학년 아동이라,
잘못된 피드백 한 번이 "나는 글을 못 쓴다"는 인식으로 남을 수 있기 때문이다.

- **확신이 낮은 판단은 아동에게 오류로 보여주지 않는다.** AI 확신도가 기준 미만인 오류 후보는
  아이 화면에서 빠지고, 보호자·교사의 검토 대상으로만 분리된다. 반복 오류 통계에도 반영되지
  않는다. 자연스러운 구어체를 틀렸다고 가르치는 것이 가장 큰 위험이다.
- **과정 데이터는 결과 이미지만큼 중요하다.** 획의 순서와 시각 정보를 잃는 최적화는 하지 않는다.
  "무엇을 썼는가" 만 남기면 이 서비스의 존재 이유가 없어진다.
- **분석이 실패해도 손글씨 원본과 획 데이터는 삭제하지 않는다.** 재시도의 근거다.
- **전문적인 진단을 제공하지 않는다.** 보호자 리포트는 지도 우선순위를 파악할 수준으로만 요약한다.

## 구성

```
WriteGrow-BE/
├── backend/     Spring Boot API 서버. 계약과 인프라를 소유한다
├── ai/          FastAPI AI 서버. OCR 과 오류 분석을 담당한다
├── deploy/      nginx 설정과 서버 배포 절차
├── docs/        백엔드 ↔ AI 계약
└── .github/     CI 와 배포 워크플로
```

두 서버는 [`docs/ai-contract.md`](docs/ai-contract.md) 로만 연결된다. 백엔드는 AI 가 없어도
동작하며(stub), AI 가 실패해도 글과 획 데이터는 보존된다.

## 주요 기능

### 자유 글쓰기 (REQ-01)

키보드와 펜 두 가지 방식으로 글을 쓴다. 키보드 글은 제출 즉시 확정되고, 손글씨 글은 AI 변환을
거친다. 아이가 쓴 원문과 최종 수정본을 모두 보존해 수정 전/후를 비교할 수 있다.

### 손글씨 OCR 과 원문 확인 (REQ-02)

```
글 시작 → 작성 중 획 데이터 전송(여러 번, 멱등) → 렌더 이미지 업로드
       → 제출 → AI 분석(비동기) → 아이가 변환 텍스트 확인·수정 → 확정
```

획 데이터는 작성 중 배치로 나눠 받아 제출 시 하나의 문서로 병합한다. AI 에는 파일이 아니라
presigned URL 만 넘긴다. 변환 결과가 마음에 들지 않으면 **다시 쓰기**로 되돌아갈 수 있는데,
이때도 이전 획은 지우지 않고 시도 번호로만 구분한다.

### 확신도 기반 오류 분석과 반복 오류 프로필 (REQ-03)

최종본이 확정되면 오류 분석이 시작된다. 맞춤법·띄어쓰기·받침·조사어미·문장구성·어휘표현
여섯 가지로 분류하고, 각 후보에 확신도를 매긴다.

| 확신도 | 아이 화면 | 보호자 검토 | 반복 오류 통계 |
| :--- | :--- | :--- | :--- |
| 기준 이상 | 노출 | — | 반영 |
| 기준 미만 | **노출 안 함** | 검토 대상 | **반영 안 함** |

아이 응답에는 낮은 확신도 후보의 개수조차 담지 않는다. "숨겨진 오류가 더 있다"고 인지하는 것
자체를 막기 위해서다.

### 보호자 주간 성장 리포트 (REQ-06)

자녀별 주간 작성 횟수, 자기교정 현황, 반복 오류, 이전 주 대비 변화 추이, 다음 집중 영역을
제공한다. 글이 없는 주는 작성 공백을 명시하고, 일자별 추이는 쓰지 않은 날도 0 으로 채워
일곱 칸을 모두 돌려준다.

### 아직 구현하지 않은 것

REQ-04(단계적 힌트), REQ-05, REQ-07, REQ-08(인증·보호자 동의), 교사 대시보드.
후속 요구사항이 바로 쓸 수 있도록 `Profile.consentConfirmed`, `ActivityEvent` 같은 자리는
지금 스키마에 마련되어 있다.

## 기술 스택

| 영역 | 선택 | 이유 |
| :--- | :--- | :--- |
| 백엔드 | Spring Boot 4.1 / Java 21 | |
| 데이터베이스 | PostgreSQL 16 + Flyway | 획 데이터와 이벤트 페이로드를 `jsonb` 로 다룬다 |
| 저장소 | AWS S3 | 손글씨 이미지와 병합된 획 문서. 퍼블릭 접근 차단, presigned URL 로만 열람 |
| AI 서버 | FastAPI / Python 3.10 | |
| OCR·오류 분석 | OpenAI GPT-4o | OCR 확신도는 모델 자기평가가 아니라 `logprobs` 로 계산한다 |
| 문서 | springdoc-openapi | |
| 테스트 | JUnit 5, Mockito, Testcontainers | |
| 배포 | Docker, GitHub Actions, GHCR, nginx | |

## API

전체 명세는 Swagger 로 확인한다. 응답은 `ApiResponse<T>` 로 통일하고 오류만 `ErrorResponse` 를 쓴다.

### 아동

| 메서드 | 경로 | 설명 |
| :--- | :--- | :--- |
| `GET` | `/api/writings/today` | 오늘 작성 현황 |
| `POST` | `/api/writings` | 글쓰기 시작 |
| `PATCH` | `/api/writings/{id}` | 임시 저장 (키보드) |
| `POST` | `/api/writings/{id}/strokes` | 획 데이터 전송 (멱등) |
| `POST` | `/api/writings/{id}/handwriting-image` | 렌더 이미지 업로드 |
| `POST` | `/api/writings/{id}/submit` | 제출 |
| `GET` | `/api/writings/{id}/analysis` | 변환 결과 폴링 |
| `PATCH` | `/api/writings/{id}/text` | 변환 텍스트 확인·확정 |
| `POST` | `/api/writings/{id}/rewrite` | 다시 쓰기 |
| `POST` | `/api/writings/{id}/analysis/retry` | 분석 재시도 |
| `GET` | `/api/writings` | 이전 글 목록 |
| `GET` | `/api/writings/{id}` | 글 상세 |
| `GET` | `/api/writings/{id}/errors` | 확정 오류 (낮은 확신도 제외) |

### 보호자·교사

| 메서드 | 경로 | 설명 |
| :--- | :--- | :--- |
| `GET` | `/api/parents/home` | 자녀별 이번 주 현황 |
| `GET` | `/api/children/{id}/weekly-report` | 주간 성장 리포트 |
| `GET` | `/api/children/{id}/writings` | 자녀 글 목록 |
| `GET` | `/api/children/{id}/writings/{writingId}` | 수정 전후 열람 |
| `GET` | `/api/children/{id}/error-profile` | 반복 오류 프로필 |
| `GET` | `/api/writings/{id}/error-review` | 낮은 확신도 검토 대상 |

### 계정

| 메서드 | 경로 | 설명 |
| :--- | :--- | :--- |
| `POST` | `/api/accounts` | 계정 생성 |
| `POST` | `/api/accounts/{id}/profiles` | 프로필 생성 |
| `GET` | `/api/accounts/{id}/profiles` | 계정의 프로필 목록 |
| `GET` | `/api/profiles/{id}` | 프로필 조회 |

## 인증에 대한 임시 규칙

MVP 는 로그인·회원가입을 구현하지 않는다. 대신 **`X-Profile-Id` 헤더**로 요청 주체를 식별하며,
프론트는 인터셉터로 자동 부착한다.

**인증이 없어도 소유권 규칙은 지금부터 지킨다.** 본인 글만 접근할 수 있고, 보호자는 같은 계정의
아동만 열람할 수 있다. 아동은 형제자매의 기록도 볼 수 없다. 인증을 붙일 때는 헤더 대신 토큰에서
프로필 ID 를 꺼내도록 리졸버만 바꾸면 되고, 컨트롤러와 서비스는 손대지 않는다.

## 로컬 실행

### 백엔드

```bash
docker compose up -d          # Postgres
cd backend && ./gradlew bootRun
```

환경 변수 `S3_BUCKET`, `S3_ACCESS_KEY`, `S3_SECRET_KEY` 가 필요하다. 없으면 기동 시 버킷 확인에서
실패한다 — 설정 오류가 아이의 제출 시점에야 드러나는 것을 막기 위해 일부러 그렇게 했다.

Swagger UI: http://localhost:8080/swagger-ui.html

AI 서버 없이 전체 흐름을 보려면 `writegrow.ai.stub=true`(dev 기본값)로 두면 된다.

### AI 서버

[`ai/README.md`](ai/README.md) 참고. `OPENAI_API_KEY` 가 필요하며, 없으면 서버는 뜨지만
OCR 과 오류 분석만 실패한다.

## 배포

공인 IP 한 대에 Postgres·백엔드·AI 를 컨테이너로 함께 올리고, 호스트 nginx 가 HTTPS 를 종단한다.
`main` 에 머지되면 테스트 → 이미지 빌드(GHCR) → 서버 배포까지 자동으로 이어진다.

절차와 필요한 시크릿은 [`deploy/README.md`](deploy/README.md) 에 있다.
