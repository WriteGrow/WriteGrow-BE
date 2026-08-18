from unittest.mock import AsyncMock, patch

from fastapi.testclient import TestClient

from app.fetch import FetchError
from app.main import app
from app.ocr import OcrError, OcrResult
from app.schemas import Segment, Stroke, StrokeDocument

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


@patch("app.main.recognize", new_callable=AsyncMock)
@patch("app.main.fetch_image_bytes", new_callable=AsyncMock)
@patch("app.main.fetch_stroke_document", new_callable=AsyncMock)
def test_analyze_combines_ocr_result_and_process_metric(
    mock_fetch_stroke, mock_fetch_image, mock_recognize
):
    """OCR 결과와 stroke 기반 processMetric 이 계약서 JSON 필드명(camelCase) 그대로 합쳐져야 한다."""
    mock_fetch_stroke.return_value = _sample_doc()
    mock_fetch_image.return_value = b"fake-png-bytes"
    mock_recognize.return_value = OcrResult(
        full_text="오늘 학교",
        overall_confidence=0.9,
        segments=[Segment(text="오늘", confidence=0.95, start_index=0, end_index=2)],
    )

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
    assert body["fullText"] == "오늘 학교"
    assert body["segments"][0]["startIndex"] == 0
    assert body["processMetric"]["totalDurationMs"] == 92000
    assert body["processMetric"]["pauseCount"] == 1  # 900ms 간격 하나
    assert body["processMetric"]["longestPauseMs"] == 900


@patch("app.main.recognize", new_callable=AsyncMock)
@patch("app.main.fetch_image_bytes", new_callable=AsyncMock)
@patch("app.main.fetch_stroke_document", new_callable=AsyncMock)
def test_analyze_falls_back_to_empty_text_when_ocr_fails(
    mock_fetch_stroke, mock_fetch_image, mock_recognize
):
    """OCR 이 실패해도 processMetric 은 그대로 내려가고, fullText 만 비워 ANALYSIS_FAILED 로 넘긴다."""
    mock_fetch_stroke.return_value = _sample_doc()
    mock_fetch_image.return_value = b"fake-png-bytes"
    mock_recognize.side_effect = OcrError("boom")

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
    assert body["fullText"] == ""
    assert body["processMetric"]["totalDurationMs"] == 92000


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
