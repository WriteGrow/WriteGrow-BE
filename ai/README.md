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
 [3] hesitation.py       (예정) stroke 클러스터를 OCR 텍스트의 글자와 정렬 → hesitationPoints
        │
        ▼
 [4] ocr.py              비전 LLM(GPT-4o) 호출 → fullText / segments(+confidence)
        │
        ▼
   schemas.AnalyzeResponse 로 조립해서 반환
```

`[3]`(hesitation.py)은 아직 자리가 안 잡혀 있다. `[4]`(OCR)는 구현되어 있지만
`OPENAI_API_KEY` 가 없거나 호출이 실패하면 `fullText` 를 빈 문자열로 돌려주는데,
계약서 규칙상 백엔드가 이걸 `ANALYSIS_FAILED` 로 처리하고 아이는
`/api/writings/{id}/analysis/retry` 로 재시도할 수 있으므로 "실패해도 안전하게 실패"한다.
`processMetric` 은 OCR 성공/실패와 무관하게 항상 채워진다.

OCR 은 GPT-4o 의 `logprobs` (모델이 실제로 계산한 토큰별 확률)를 그대로 이용해
`segments[].confidence` 를 계산한다 — 모델에게 확신도를 다시 물어보는 자기평가 방식이
아니다. 한글은 토큰 경계와 글자 경계가 안 맞을 수 있어서, `ocr.py::_tokens_to_chars` 가
바이트를 다시 이어붙여 글자 단위로 복원하는 과정을 거친다.

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

backend 쪽에서 이 서버에 실제로 붙여보려면 `application-dev.yml` 의
`writegrow.ai.stub` 을 `false` 로 바꾼다 (base-url 은 이미 `http://localhost:8000` 로 일치).

## 설정

환경 변수(`WRITEGROW_AI_` 접두사, `app/config.py`):

| 변수 | 기본값 | 설명 |
| :--- | :--- | :--- |
| `WRITEGROW_AI_PAUSE_THRESHOLD_MS` | 600 | 이보다 짧은 pen-up 간격은 멈춤으로 세지 않는다 |
| `WRITEGROW_AI_FETCH_CONNECT_TIMEOUT_S` | 5.0 | imageUrl/strokeUrl 다운로드 connect 타임아웃 |
| `WRITEGROW_AI_FETCH_READ_TIMEOUT_S` | 20.0 | 다운로드 read 타임아웃 |
| `WRITEGROW_AI_OCR_MODEL` | gpt-4o | OCR 에 쓸 모델 |
| `WRITEGROW_AI_ERROR_ANALYSIS_MODEL` | gpt-4o | 오류 분석(`/text/analyze`)에 쓸 모델 |

접두사 없는 변수 (OpenAI SDK가 직접 읽음, `.env` 참고):

| 변수 | 설명 |
| :--- | :--- |
| `OPENAI_API_KEY` | platform.openai.com 에서 발급. 없으면 OCR 만 실패 처리되고 나머지는 정상 동작 |
