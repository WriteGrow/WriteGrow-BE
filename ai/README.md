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

backend 쪽에서 이 서버에 실제로 붙여보려면 `application-dev.yml` 의
`writegrow.ai.stub` 을 `false` 로 바꾼다 (base-url 은 이미 `http://localhost:8000` 로 일치).

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

접두사 없는 변수 (OpenAI SDK가 직접 읽음, `.env` 참고):

| 변수 | 설명 |
| :--- | :--- |
| `OPENAI_API_KEY` | platform.openai.com 에서 발급. 없으면 OCR 만 실패 처리되고 나머지는 정상 동작 |
