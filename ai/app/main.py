"""
docs/ai-contract.md 가 정의하는 AI 서버. 진입점은 POST /handwriting/analyze 하나.

파이프라인 순서 (각 단계 파일):
  1. fetch.py           imageUrl/strokeUrl 다운로드
  2. stroke_metrics.py  획 시계열 → 과정 지표(순수 알고리즘, AI 아님)
  3. hesitation.py       (다음 라운드) stroke 클러스터 ↔ 글자 정렬 → hesitationPoints
  4. ocr.py              (다음 라운드) 비전 LLM 호출 → fullText/segments
"""

from __future__ import annotations

import asyncio
import logging

from fastapi import FastAPI, HTTPException

from app.config import settings
from app.fetch import FetchError, fetch_image_bytes, fetch_stroke_document
from app.ocr import recognize
from app.schemas import AnalyzeRequest, AnalyzeResponse
from app.stroke_metrics import compute_process_metric

logger = logging.getLogger("writegrow.ai")

app = FastAPI(title="WriteGrow AI", version="0.1.0")


@app.get("/health")
async def health() -> dict:
    return {"status": "ok"}


@app.post("/handwriting/analyze", response_model=AnalyzeResponse)
async def analyze(request: AnalyzeRequest) -> AnalyzeResponse:
    try:
        stroke_doc, image_bytes = await asyncio.gather(
            fetch_stroke_document(request.stroke_url),
            fetch_image_bytes(request.image_url),
        )
    except FetchError as e:
        logger.warning("다운로드 실패: writingId=%s %s", request.writing_id, e)
        raise HTTPException(status_code=502, detail=str(e)) from e

    metric, _pauses = compute_process_metric(
        stroke_doc, pause_threshold_ms=settings.pause_threshold_ms
    )
    # TODO(다음 라운드): _pauses 를 hesitation.py 에 넘겨 metric.hesitation_points 를 채운다.

    try:
        ocr_result = await recognize(image_bytes, request.expected_topic)
    except NotImplementedError:
        logger.warning("OCR 미구현 — fullText 를 비워 ANALYSIS_FAILED 로 넘긴다.")
        return AnalyzeResponse(
            full_text="",
            overall_confidence=None,
            segments=[],
            process_metric=metric,
        )

    return AnalyzeResponse(
        full_text=ocr_result.full_text,
        overall_confidence=ocr_result.overall_confidence,
        segments=ocr_result.segments,
        process_metric=metric,
    )
