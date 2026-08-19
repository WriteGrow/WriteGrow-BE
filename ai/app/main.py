"""
docs/ai-contract.md 가 정의하는 AI 서버. 엔드포인트 두 개:

  POST /handwriting/analyze  손글씨 이미지+획 데이터 → 텍스트 변환 + 과정 지표
  POST /text/analyze         확정된 텍스트 → 맞춤법 등 오류 후보 (REQ-03)

입력이 완전히 달라(이미지+획 vs 텍스트만) 계약서가 엔드포인트를 나눴다.

/handwriting/analyze 파이프라인 순서 (각 단계 파일):
  1. fetch.py           imageUrl/strokeUrl 다운로드
  2. stroke_metrics.py  획 시계열 → 과정 지표(순수 알고리즘, AI 아님)
  3. hesitation.py       stroke 클러스터 ↔ 글자 정렬 → hesitationPoints (OCR 결과가 있어야 가능)
  4. ocr.py              비전 LLM(GPT-4o) 호출 → fullText/segments

/text/analyze 는 text_analysis.py 하나로 처리한다 (이미지/획 데이터가 없어 단순함).
"""

from __future__ import annotations

import asyncio
import logging

from fastapi import FastAPI, HTTPException

from app.config import settings
from app.fetch import FetchError, fetch_image_bytes, fetch_stroke_document
from app.hesitation import compute_hesitation_points
from app.ocr import OcrError, recognize
from app.schemas import AnalyzeRequest, AnalyzeResponse, TextAnalyzeRequest, TextAnalyzeResponse
from app.stroke_metrics import compute_process_metric
from app.text_analysis import TextAnalysisError, analyze_text

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


@app.post("/text/analyze", response_model=TextAnalyzeResponse)
async def analyze_text_endpoint(request: TextAnalyzeRequest) -> TextAnalyzeResponse:
    try:
        errors = await analyze_text(request.text, request.topic)
    except TextAnalysisError as e:
        # 빈 errors 배열은 "오류 없음"이라는 정상 응답이라, OCR 처럼 실패를 빈 값으로
        # 흉내내면 안 된다. 실패는 502 로 알려서 백엔드가 교정 안내를 만들지 않게 한다.
        logger.warning("오류 분석 실패: writingId=%s %s", request.writing_id, e)
        raise HTTPException(status_code=502, detail=str(e)) from e

    return TextAnalyzeResponse(errors=errors)
