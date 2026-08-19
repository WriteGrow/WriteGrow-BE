"""
stroke_capture.html 로 받은 image.png + strokes.json 을 로컬 파일로 바로 넣어서
OCR → processMetric → hesitationPoints 전체 파이프라인을 확인하는 스크립트.

S3 업로드나 백엔드 없이, imageUrl/strokeUrl 다운로드 단계(fetch.py)만 건너뛰고
나머지(ocr.py, stroke_metrics.py, hesitation.py)는 실제 서버(main.py)와
완전히 같은 코드를 그대로 탄다.

사용법:
    ai/.venv/Scripts/python scripts/try_handwriting_local.py image.png strokes.json
"""

from __future__ import annotations

import asyncio
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.config import settings  # noqa: E402
from app.hesitation import compute_hesitation_points  # noqa: E402
from app.ocr import OcrError, recognize  # noqa: E402
from app.schemas import StrokeDocument  # noqa: E402
from app.stroke_metrics import compute_process_metric  # noqa: E402


async def main() -> None:
    if len(sys.argv) != 3:
        print("사용법: python scripts/try_handwriting_local.py <image.png> <strokes.json>")
        raise SystemExit(1)

    image_path, stroke_path = Path(sys.argv[1]), Path(sys.argv[2])
    image_bytes = image_path.read_bytes()
    stroke_doc = StrokeDocument.model_validate(json.loads(stroke_path.read_text(encoding="utf-8")))

    metric, pauses = compute_process_metric(stroke_doc, pause_threshold_ms=settings.pause_threshold_ms)

    print("=== stroke 원자료 ===")
    print(f"strokeCount={len(stroke_doc.strokes)}")
    print(f"pauses (임계값 무관, 전부): {[p.gap_ms for p in pauses]}")
    print()

    try:
        ocr_result = await recognize(image_bytes, expected_topic=None)
    except OcrError as e:
        print(f"OCR 실패: {e}")
        print("→ ai/.env 에 OPENAI_API_KEY 확인. hesitationPoints 는 fullText 없이는 계산 못 함.")
        return

    print("=== OCR ===")
    print(f"fullText: {ocr_result.full_text!r}")
    print(f"overallConfidence: {ocr_result.overall_confidence}")
    for seg in ocr_result.segments:
        print(f"  - {seg.text!r:10} confidence={seg.confidence}")
    print()

    metric.hesitation_points = compute_hesitation_points(stroke_doc.strokes, ocr_result.full_text)

    print("=== processMetric ===")
    print(f"totalDurationMs={metric.total_duration_ms}")
    print(f"pauseCount={metric.pause_count}")
    print(f"longestPauseMs={metric.longest_pause_ms}")
    print(f"avgStrokeDurationMs={metric.avg_stroke_duration_ms}")
    print("hesitationPoints:")
    if not metric.hesitation_points:
        print("  (없음)")
    for hp in metric.hesitation_points:
        print(
            f"  - charIndex={hp.char_index} char={hp.char!r} jamo={hp.jamo} "
            f"durationMs={hp.duration_ms} retryCount={hp.retry_count}"
        )


if __name__ == "__main__":
    asyncio.run(main())
