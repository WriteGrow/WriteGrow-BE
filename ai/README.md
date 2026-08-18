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
 [4] ocr.py              (예정) 비전 LLM 호출 → fullText / segments(+confidence)
        │
        ▼
   schemas.AnalyzeResponse 로 조립해서 반환
```

`[3]`, `[4]` 는 아직 자리만 잡혀 있다(`ocr.py::recognize` 는 `NotImplementedError`).
그동안은 `fullText` 를 빈 문자열로 돌려주는데, 계약서 규칙상 백엔드가 이걸
`ANALYSIS_FAILED` 로 처리하고 아이는 `/api/writings/{id}/analysis/retry` 로 재시도할 수 있으므로
"미구현 상태에서도 안전하게 실패"한다. `processMetric` 은 OCR 없이도 이미 채워진다.

## 로컬 실행

```bash
py -3.10 -m venv .venv
./.venv/Scripts/pip install -r requirements.txt

# 테스트
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
| `WRITEGROW_AI_FETCH_CONNECT_TIMEOUT_S` | 5.0 | imageUrl/strokeUrl 다운로드 connect 타임아웃 |
| `WRITEGROW_AI_FETCH_READ_TIMEOUT_S` | 20.0 | 다운로드 read 타임아웃 |
