"""
텍스트 하나로 오류 분석(text_analysis.py)만 빠르게 확인하는 스크립트.

사용법:
    ai/.venv/Scripts/python scripts/try_text_analyze.py "오늘 학교에서 친구랑 놀앗다"

.env 에 OPENAI_API_KEY 가 채워져 있어야 한다.
"""

from __future__ import annotations

import asyncio
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from app.text_analysis import TextAnalysisError, analyze_text  # noqa: E402


async def main() -> None:
    if len(sys.argv) != 2:
        print('사용법: python scripts/try_text_analyze.py "아이가 쓴 문장"')
        raise SystemExit(1)

    text = sys.argv[1]

    try:
        errors = await analyze_text(text, topic=None)
    except TextAnalysisError as e:
        print(f"오류 분석 실패: {e}")
        raise SystemExit(1)

    if not errors:
        print("오류 없음 (정상)")
        return

    for err in errors:
        print(f"[{err.type}] {err.original!r} → {err.suggestion!r}")
        print(f"  confidence={err.confidence}  위치=({err.start_index}, {err.end_index})")
        print(f"  이유: {err.reason}")


if __name__ == "__main__":
    asyncio.run(main())
