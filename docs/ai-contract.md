# 백엔드 ↔ AI 서버 연동 계약

이 문서는 `backend` 가 `ai` 서버를 호출할 때 사용하는 인터페이스를 정의한다.
백엔드 구현은 `backend/src/main/java/com/example/writegrow/infra/ai/` 에 있다.

## 전제

- 손글씨 **이미지와 획(stroke) 데이터는 백엔드가 S3 에 저장**한 뒤, 임시 열람 URL(presigned, 기본 10분)만 전달한다.
  AI 서버는 S3 자격 증명이 필요 없고, 두 URL 을 GET 으로 내려받기만 하면 된다.
- 이 서비스의 차별점은 **결과가 아니라 과정을 본다**는 것이다. 이미지 한 장만으로는 알 수 없는
  "어디에서 느려졌는가", "어떤 자모를 어려워하는가"를 획 데이터의 시각 정보로 판단해 달라.
- 백엔드는 응답의 모르는 필드를 무시하므로, AI 팀이 필드를 추가해도 백엔드는 깨지지 않는다.

## 엔드포인트

```
POST {writegrow.ai.base-url}/handwriting/analyze
Content-Type: application/json
```

base-url 은 dev 프로파일에서 `http://localhost:8000`, prod 에서는 `AI_BASE_URL` 환경 변수로 지정한다.
연결 타임아웃 3초, 읽기 타임아웃 60초.

## 요청

```json
{
  "writingId": 123,
  "imageUrl": "https://s3.../handwriting/2026/08/09/123/image-uuid.png?X-Amz-...",
  "strokeUrl": "https://s3.../handwriting/2026/08/09/123/strokes-uuid.json?X-Amz-...",
  "canvas": { "width": 1024, "height": 768 },
  "expectedTopic": "오늘 있었던 일"
}
```

| 필드 | 설명 |
| :--- | :--- |
| `writingId` | 글 식별자. 응답 매칭이 아니라 로깅/디버깅용 |
| `imageUrl` | 손글씨 렌더 이미지(PNG/JPEG) |
| `strokeUrl` | 아래 "획 데이터 문서" JSON |
| `canvas` | 좌표 정규화를 위한 캔버스 크기. 클라이언트가 안 보냈으면 null |
| `expectedTopic` | 아동이 고른 글쓰기 주제. 문맥 기반 보정에 참고 (없으면 null) |

## 획 데이터 문서 (`strokeUrl` 의 내용)

```json
{
  "writingId": 123,
  "canvasWidth": 1024,
  "canvasHeight": 768,
  "strokeCount": 48,
  "totalDurationMs": 92000,
  "strokes": [
    {
      "index": 0,
      "penDownAt": 1200,
      "penUpAt": 1620,
      "points": [
        { "x": 132.5, "y": 88.0, "t": 1200, "pressure": 0.42 }
      ]
    }
  ]
}
```

- `t`, `penDownAt`, `penUpAt` 은 **글쓰기 세션 시작 기준 경과 시간(ms)** 이다.
- `strokes` 는 `index` 오름차순, 즉 실제로 그은 순서로 정렬되어 있다.
- `pressure` 는 기기가 지원하지 않으면 null 이다.

## 응답

```json
{
  "fullText": "오늘 학교에서 친구랑 놀앗다",
  "overallConfidence": 0.91,
  "segments": [
    { "text": "오늘", "confidence": 0.97, "startIndex": 0, "endIndex": 2 },
    { "text": "놀앗다", "confidence": 0.62, "startIndex": 12, "endIndex": 15 }
  ],
  "processMetric": {
    "totalDurationMs": 92000,
    "pauseCount": 4,
    "longestPauseMs": 7300,
    "avgStrokeDurationMs": 410,
    "hesitationPoints": [
      { "charIndex": 12, "char": "놀", "jamo": "ㄴ", "durationMs": 5200, "retryCount": 2 }
    ]
  }
}
```

| 필드 | 필수 | 설명 |
| :--- | :--- | :--- |
| `fullText` | ✅ | 변환된 전체 텍스트. 비어 있으면 백엔드가 실패로 처리한다 |
| `overallConfidence` | | 0.0 ~ 1.0 |
| `segments[].confidence` | | **0.0 ~ 1.0. 백엔드는 이 값이 임계값(기본 0.7) 미만이면 해당 구절을 아동 교정 대상에서 제외한다.** 자신 없는 구절은 낮은 값을 정직하게 내려 달라 |
| `segments[].startIndex/endIndex` | | `fullText` 내 위치 |
| `processMetric` | | 과정 분석 지표. 없으면 저장하지 않고 넘어간다 |
| `hesitationPoints[].char` | | 자바 예약어라 백엔드 내부에서는 `character` 로 매핑한다 |

## 오류 처리

- 2xx 가 아니거나 `fullText` 가 비면 백엔드는 해당 글을 `ANALYSIS_FAILED` 로 기록한다.
- **실패해도 S3 의 이미지·획 데이터는 삭제하지 않는다.** 아동은 재작성 없이
  `POST /api/writings/{id}/analysis/retry` 로 재시도할 수 있다.

## AI 서버가 없을 때

백엔드는 `writegrow.ai.stub=true`(dev 프로파일 기본값)일 때 `StubAiAnalysisClient` 로 고정 응답을 돌려준다.
낮은 확신도 구절이 하나 포함되어 있어 임계값 처리와 아동 수정 흐름을 그대로 확인할 수 있다.

AI 서버가 준비되면 `application-dev.yml` 의 `writegrow.ai.stub` 을 `false` 로 바꾸고 `base-url` 만 맞추면 된다.
