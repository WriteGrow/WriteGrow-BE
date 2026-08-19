"""
로컬 이미지 파일 하나로 OCR 단계(ocr.py)만 빠르게 확인하는 스크립트.
전체 /handwriting/analyze 엔드포인트(이미지+stroke 둘 다 필요)와 달리
손글씨 사진 한 장만 있으면 된다.

사용법:
    ai/.venv/Scripts/python scripts/try_ocr.py <이미지 경로>

.env 에 OPENAI_API_KEY 가 채워져 있어야 한다.
"""

from __future__ import annotations

import asyncio
import sys
from pathlib import Path

# ai/ 를 sys.path 에 넣는다 — 이 스크립트를 어느 위치에서 실행하든 `app` 패키지를 찾게 하려고.
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.ocr import OcrError, recognize  # noqa: E402 (경로 등록 뒤에 import 해야 함)


async def main() -> None:
    if len(sys.argv) != 2:
        print("사용법: python scripts/try_ocr.py <이미지 경로>")
        raise SystemExit(1)

    path = Path(sys.argv[1])
    image_bytes = path.read_bytes()

    try:
        result = await recognize(image_bytes, expected_topic=None)
    except OcrError as e:
        print(f"OCR 실패: {e}")
        print("→ ai/.env 에 OPENAI_API_KEY 가 제대로 들어있는지 확인해보세요.")
        raise SystemExit(1)

    print(f"fullText: {result.full_text!r}")
    print(f"overallConfidence: {result.overall_confidence}")
    for seg in result.segments:
        print(f"  - {seg.text!r:10} confidence={seg.confidence}")


if __name__ == "__main__":
    asyncio.run(main())
