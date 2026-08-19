# WriteGrow AI 서버

`backend` 가 호출하는 AI 서버. 인터페이스는 [`docs/ai-contract.md`](../docs/ai-contract.md) 를 따른다.

## 파이프라인 구조

```
POST /handwriting/analyze
        │
        ▼
 [1] fetch.py            imageUrl / strokeUrl 다운로드 (S3 자격 증명 불필요, GET 만 하면 됨)
        │
        ▼
 [2] stroke_metrics.py   획(stroke) 시계열 → 과정 지표 계산 (순수 알고리즘, 모델 없음)
        │                totalDurationMs / pauseCount / longestPauseMs / avgStrokeDurationMs
        ▼
 [3] hesitation.py       stroke 클러스터를 OCR 텍스트의 글자와 정렬 → hesitationPoints
        │
        ▼
 [4] ocr.py              비전 LLM(GPT-4o) 호출 → fullText / segments(+confidence)
        │
        ▼
   schemas.AnalyzeResponse 로 조립해서 반환
```

`[4]`(OCR)는 `OPENAI_API_KEY` 가 없거나 호출이 실패하면 `fullText` 를 빈 문자열로
돌려주는데, 계약서 규칙상 백엔드가 이걸 `ANALYSIS_FAILED` 로 처리하고 아이는
`/api/writings/{id}/analysis/retry` 로 재시도할 수 있으므로 "실패해도 안전하게 실패"한다.
`processMetric` 의 집계값(pauseCount 등)은 OCR 성공/실패와 무관하게 항상 채워지고,
`hesitationPoints` 는 OCR 이 성공해 글자 순서를 알아야만 채워진다.

OCR 은 GPT-4o 의 `logprobs` (모델이 실제로 계산한 토큰별 확률)를 그대로 이용해
`segments[].confidence` 를 계산한다 — 모델에게 확신도를 다시 물어보는 자기평가 방식이
아니다. 한글은 토큰 경계와 글자 경계가 안 맞을 수 있어서, `ocr.py::_tokens_to_chars` 가
바이트를 다시 이어붙여 글자 단위로 복원하는 과정을 거친다.

hesitation 은 stroke 사이 간격으로 "한 글자를 쓰는 동안 그은 뭉치"를 나누고, 그 뭉치
순서를 fullText 의 글자 순서와 비례 정렬한다. jamo 는 정밀한 좌표 분석 대신 해당
글자의 초성으로 근사한다 — 자세한 한계는 `hesitation.py` 상단 docstring 참고.

```
POST /text/analyze (REQ-03)
        │
        ▼
 text_analysis.py   GPT 구조화 출력(JSON Schema strict)으로 오류 후보 생성
                     → startIndex/endIndex 는 모델이 세지 않고, 모델이 복사한
                       원문 조각(original)을 서버가 text 에서 직접 찾아 계산
```

여긴 OCR 과 confidence 의 성격이 다르다 — "이 표현이 오류인가"는 판단 문제라 logprobs
가 없다. 그래서 모델 자기평가 confidence 를 쓰되, 계약서의 "애매하면 낮게" 원칙을
프롬프트에 그대로 명시한다. 실패 시 빈 배열을 흉내내지 않고 502 를 돌려준다 — 빈
배열은 "오류 없음"이라는 정상 응답이라 실패와 구분해야 하기 때문이다.

## 로컬 실행

```bash
py -3.10 -m venv .venv
./.venv/Scripts/pip install -r requirements.txt

# OCR 을 실제로 호출하려면 (없어도 서버는 뜨고, OCR 만 실패로 처리됨)
cp .env.example .env
# .env 열어서 OPENAI_API_KEY 채우기

# 테스트 (OCR 관련 테스트는 API 를 실제로 호출하지 않고 로직만 검증한다)
./.venv/Scripts/python -m pytest -q

# 서버 (기본 포트 8000, backend 의 dev 기본 base-url 과 일치)
./.venv/Scripts/python -m uvicorn app.main:app --reload
```

`http://localhost:8000/docs` 에서 FastAPI 가 자동 생성한 Swagger UI 로 바로 확인 가능하다.

전체 엔드포인트 대신 모델 호출 부분만 빠르게 확인하고 싶으면:

```bash
./.venv/Scripts/python scripts/try_ocr.py <이미지 경로>
./.venv/Scripts/python scripts/try_text_analyze.py "아이가 쓴 문장"
```

`/handwriting/analyze` 전체(OCR+hesitation)를 S3/백엔드 없이 로컬 파일로 확인하려면
`scripts/stroke_capture.html` 을 브라우저로 열어(태블릿/스타일러스 없이 마우스로도 됨)
직접 몇 글자 써서 `image.png`+`strokes.json` 을 내려받은 뒤:

```bash
./.venv/Scripts/python scripts/try_handwriting_local.py image.png strokes.json
```

backend 쪽에서 이 서버에 실제로 붙여보려면 `application-dev.yml` 의
`writegrow.ai.stub` 을 `false` 로 바꾼다 (base-url 은 이미 `http://localhost:8000` 로 일치).

## 배포/연동 체크리스트 (backend·인프라 팀 참고)

- **base-url**: dev `http://localhost:8000` 고정, prod 는 `AI_BASE_URL` 환경 변수로 지정
- **실제 AI 로 전환**: `writegrow.ai.stub` 을 `false` 로, `base-url`/`AI_BASE_URL` 을 이 서버
  주소로 맞추기. 이 두 개를 빼먹으면 backend 는 항상 `StubAiAnalysisClient` 고정 응답만
  본다 — 실제로 겪었던 문제라 다시 강조한다.
- **필수 비밀 값**: `OPENAI_API_KEY`. 이미지에는 안 들어있고 컨테이너 실행 시 환경 변수로
  주입해야 한다. 없으면 서버는 정상 기동하지만 OCR/오류 분석 호출만 실패한다.
- **컨테이너**: `ai/Dockerfile` 로 빌드 가능(`docker build -t writegrow-ai ai/`). 포트 8000,
  `GET /health` 로 헬스체크. `docker-compose.prod.yml`/배포 워크플로에 서비스로 등록하는
  건 별도 작업(이 저장소 기준 아직 안 되어 있음).
- **두 엔드포인트의 실패 처리 방식이 다르다** — 계약서 규칙을 그대로 따른 것이니 backend
  쪽에서 혼동하지 않게 참고:
  - `/handwriting/analyze` 실패 → **200 + `fullText` 빈 문자열** (재시도 가능한 실패)
  - `/text/analyze` 실패 → **502** (빈 `errors` 배열은 "오류 없음"이라는 정상 응답이라
    실패와 구분해야 하기 때문에 흉내내지 않는다)

## 알려진 한계

- **hesitation.py 는 실제 태블릿/스타일러스 데이터로 검증된 적이 없다.** 지금까지는
  `stroke_capture.html`(마우스 캡처)로만 확인했다. 마우스는 실제 펜보다 stroke 가 잘게
  쪼개지는 경향이 있어 `retryCount` 가 실제보다 높게 나올 수 있다 — 진짜 태블릿 데이터가
  생기면 다시 한번 확인이 필요하다.
- **jamo 는 정밀 좌표 분석이 아니라 해당 글자의 초성으로 근사한 값**이다.
- 기획안에 있던 "여러 글 누적 오류 프로필 요약", "선생님처럼 줄글로 피드백" 같은 기능은
  현재 `docs/ai-contract.md` 범위 밖이다. 필요하면 REQ-03 처럼 별도 스펙 논의 후 추가한다.

## 설정

환경 변수(`WRITEGROW_AI_` 접두사, `app/config.py`):

| 변수 | 기본값 | 설명 |
| :--- | :--- | :--- |
| `WRITEGROW_AI_PAUSE_THRESHOLD_MS` | 600 | 이보다 짧은 pen-up 간격은 멈춤으로 세지 않는다 |
| `WRITEGROW_AI_INTRA_CHAR_GAP_MS` | 400 | 이보다 짧은 간격은 "같은 글자를 계속 쓰는 중"으로 본다 |
| `WRITEGROW_AI_HESITATION_PAUSE_MS` | 1500 | 이보다 길게 멈춰야 hesitationPoints 에 보고한다 |
| `WRITEGROW_AI_FETCH_CONNECT_TIMEOUT_S` | 5.0 | imageUrl/strokeUrl 다운로드 connect 타임아웃 |
| `WRITEGROW_AI_FETCH_READ_TIMEOUT_S` | 20.0 | 다운로드 read 타임아웃 |
| `WRITEGROW_AI_OCR_MODEL` | gpt-4o | OCR 에 쓸 모델 |
| `WRITEGROW_AI_ERROR_ANALYSIS_MODEL` | gpt-4o | 오류 분석(`/text/analyze`)에 쓸 모델 |

접두사 없는 변수 (OpenAI SDK가 직접 읽음, `.env` 참고):

| 변수 | 설명 |
| :--- | :--- |
| `OPENAI_API_KEY` | platform.openai.com 에서 발급. 없으면 OCR 만 실패 처리되고 나머지는 정상 동작 |
