from unittest.mock import AsyncMock, patch

from fastapi.testclient import TestClient

from app.fetch import FetchError
from app.main import app
from app.ocr import OcrError, OcrResult
from app.schemas import ErrorItem, Segment, Stroke, StrokeDocument
from app.text_analysis import TextAnalysisError

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


@patch("app.main.recognize", new_callable=AsyncMock)
@patch("app.main.fetch_image_bytes", new_callable=AsyncMock)
@patch("app.main.fetch_stroke_document", new_callable=AsyncMock)
def test_analyze_populates_hesitation_points(mock_fetch_stroke, mock_fetch_image, mock_recognize):
    """OCR 이 성공하면, stroke 클러스터를 fullText 글자와 정렬해 hesitationPoints 를 채운다."""
    mock_fetch_stroke.return_value = StrokeDocument(
        writing_id=123,
        strokes=[
            Stroke(index=0, pen_down_at=0, pen_up_at=200, points=[]),
            # 2000ms 멈춘 뒤 3개의 stroke(재시도 의심)로 "늘"을 씀
            Stroke(index=1, pen_down_at=2200, pen_up_at=2350, points=[]),
            Stroke(index=2, pen_down_at=2400, pen_up_at=2550, points=[]),
            Stroke(index=3, pen_down_at=2600, pen_up_at=2750, points=[]),
        ],
    )
    mock_fetch_image.return_value = b"fake-png-bytes"
    mock_recognize.return_value = OcrResult(full_text="오늘", overall_confidence=0.9, segments=[])

    resp = client.post(
        "/handwriting/analyze",
        json={
            "writingId": 123,
            "imageUrl": "https://example.com/image.png",
            "strokeUrl": "https://example.com/strokes.json",
        },
    )

    assert resp.status_code == 200
    points = resp.json()["processMetric"]["hesitationPoints"]
    assert len(points) == 1
    assert points[0]["char"] == "늘"
    assert points[0]["charIndex"] == 1
    assert points[0]["retryCount"] == 1


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


@patch("app.main.analyze_text", new_callable=AsyncMock)
def test_text_analyze_returns_errors_with_wire_field_names(mock_analyze_text):
    mock_analyze_text.return_value = [
        ErrorItem(
            type="FINAL_CONSONANT",
            start_index=3,
            end_index=6,
            original="놀앗다",
            suggestion="놀았다",
            confidence=0.93,
            reason="'았'의 받침 표기",
        )
    ]

    resp = client.post(
        "/text/analyze",
        json={"writingId": 123, "text": "친구랑 놀앗다"},
    )

    assert resp.status_code == 200
    errors = resp.json()["errors"]
    assert errors[0]["type"] == "FINAL_CONSONANT"
    assert errors[0]["startIndex"] == 3
    assert errors[0]["endIndex"] == 6


@patch("app.main.analyze_text", new_callable=AsyncMock)
def test_text_analyze_returns_502_on_failure_not_empty_list(mock_analyze_text):
    """빈 배열은 '오류 없음'이라는 정상 응답이라, 실패는 빈 배열이 아니라 502 여야 한다."""
    mock_analyze_text.side_effect = TextAnalysisError("boom")

    resp = client.post(
        "/text/analyze",
        json={"writingId": 123, "text": "친구랑 놀앗다"},
    )

    assert resp.status_code == 502
