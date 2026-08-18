"""
Stage 4 (다음 라운드에서 구현): 손글씨 이미지를 텍스트로 바꾸는 단계.

지금은 자리만 잡아둔다 — main.py 가 이 함수를 호출하고, 아직 구현되지 않았으면
빈 결과로 대체해서 나머지 파이프라인(fetch, 과정 지표 계산)은 먼저 눈으로
확인할 수 있게 한다. fullText 가 비면 계약서 규칙대로 백엔드가 ANALYSIS_FAILED 로
기록하므로, "미구현 상태에서도 안전하게 실패하는" 동작이다.
"""

from __future__ import annotations

from dataclasses import dataclass, field

from app.schemas import Segment


@dataclass
class OcrResult:
    full_text: str
    overall_confidence: float | None
    segments: list[Segment] = field(default_factory=list)


async def recognize(image_bytes: bytes, expected_topic: str | None) -> OcrResult:
    """비전 LLM 호출로 교체될 자리. (다음 라운드)"""
    raise NotImplementedError("OCR 단계는 아직 구현 전입니다 — 다음 라운드에서 비전 LLM으로 붙입니다.")
