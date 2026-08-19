"""
docs/ai-contract.md 가 정의하는 AI 서버. 진입점은 POST /handwriting/analyze 하나.

파이프라인 순서 (각 단계 파일):
  1. fetch.py           imageUrl/strokeUrl 다운로드
  2. stroke_metrics.py  획 시계열 → 과정 지표(순수 알고리즘, AI 아님)
  3. hesitation.py       stroke 클러스터 ↔ 글자 정렬 → hesitationPoints (OCR 결과가 있어야 가능)
  4. ocr.py              비전 LLM(GPT-4o) 호출 → fullText/segments
"""

from __future__ import annotations

import asyncio
import logging

from fastapi import FastAPI, HTTPException

from app.config import settings
from app.fetch import FetchError, fetch_image_bytes, fetch_stroke_document
from app.hesitation import compute_hesitation_points
from app.ocr import OcrError, recognize
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

    metric, _ = compute_process_metric(stroke_doc, pause_threshold_ms=settings.pause_threshold_ms)

    try:
        ocr_result = await recognize(image_bytes, request.expected_topic)
    except OcrError as e:
        # OCR 이 실패해도 processMetric 은 이미 계산되어 있다. fullText 를 비워 두면
        # 계약서 규칙대로 백엔드가 ANALYSIS_FAILED 로 기록하고, 아이는 재시도할 수 있다.
        logger.warning("OCR 실패: writingId=%s %s", request.writing_id, e)
        return AnalyzeResponse(
            full_text="",
            overall_confidence=None,
            segments=[],
            process_metric=metric,
        )

    # hesitationPoints 는 OCR 이 성공해야(글자 순서를 알아야) 계산할 수 있다.
    # 클러스터링/정렬은 근사치라 실패해도 나머지 응답(특히 fullText)까지 잃으면 안 된다.
    try:
        metric.hesitation_points = compute_hesitation_points(stroke_doc.strokes, ocr_result.full_text)
    except Exception:
        logger.exception("hesitation 계산 실패: writingId=%s", request.writing_id)

    return AnalyzeResponse(
        full_text=ocr_result.full_text,
        overall_confidence=ocr_result.overall_confidence,
        segments=ocr_result.segments,
        process_metric=metric,
    )
