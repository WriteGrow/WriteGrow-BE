from unittest.mock import AsyncMock, patch

from fastapi.testclient import TestClient

from app.fetch import FetchError
from app.main import app
from app.schemas import Stroke, StrokeDocument

client = TestClient(app)


def _sample_doc() -> StrokeDocument:
    return StrokeDocument(
        writing_id=123,
        total_duration_ms=92000,
        strokes=[
            Stroke(index=0, pen_down_at=0, pen_up_at=300, points=[]),
            Stroke(index=1, pen_down_at=1200, pen_up_at=1500, points=[]),
        ],
    )


def test_health():
    resp = client.get("/health")
    assert resp.status_code == 200
    assert resp.json() == {"status": "ok"}


@patch("app.main.fetch_image_bytes", new_callable=AsyncMock)
@patch("app.main.fetch_stroke_document", new_callable=AsyncMock)
def test_analyze_returns_process_metric_with_wire_field_names(
    mock_fetch_stroke, mock_fetch_image
):
    """OCR 이 아직 없어도, 계약서 JSON 필드명(camelCase) 그대로 processMetric 은 채워져야 한다."""
    mock_fetch_stroke.return_value = _sample_doc()
    mock_fetch_image.return_value = b"fake-png-bytes"

    resp = client.post(
        "/handwriting/analyze",
        json={
            "writingId": 123,
            "imageUrl": "https://example.com/image.png",
            "strokeUrl": "https://example.com/strokes.json",
        },
    )

    assert resp.status_code == 200
    body = resp.json()
    assert body["fullText"] == ""  # OCR 미구현 → 계약서 규칙대로 빈 값
    assert body["processMetric"]["totalDurationMs"] == 92000
    assert body["processMetric"]["pauseCount"] == 1  # 900ms 간격 하나
    assert body["processMetric"]["longestPauseMs"] == 900


@patch("app.main.fetch_image_bytes", new_callable=AsyncMock)
@patch("app.main.fetch_stroke_document", new_callable=AsyncMock)
def test_analyze_returns_502_when_download_fails(mock_fetch_stroke, mock_fetch_image):
    mock_fetch_stroke.side_effect = FetchError("https://example.com/strokes.json", RuntimeError("boom"))
    mock_fetch_image.return_value = b"fake-png-bytes"

    resp = client.post(
        "/handwriting/analyze",
        json={
            "writingId": 123,
            "imageUrl": "https://example.com/image.png",
            "strokeUrl": "https://example.com/strokes.json",
        },
    )

    assert resp.status_code == 502
