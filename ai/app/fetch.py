"""
Stage 1: 백엔드가 준 presigned URL 두 개(imageUrl, strokeUrl)를 실제로 내려받는다.

AI 서버는 S3 자격 증명이 없다 — 그냥 GET 으로 받아오면 되는 임시 URL 이기 때문이다
(docs/ai-contract.md 전제 부분 참고).
"""

from __future__ import annotations

import httpx

from app.config import settings
from app.schemas import StrokeDocument


class FetchError(Exception):
    """이미지/획 데이터 다운로드 실패. 원인(url, 원본 예외)을 그대로 들고 있는다."""

    def __init__(self, url: str, cause: Exception):
        super().__init__(f"{url} 다운로드 실패: {cause!r}")
        self.url = url
        self.cause = cause


def _timeout() -> httpx.Timeout:
    return httpx.Timeout(
        connect=settings.fetch_connect_timeout_s,
        read=settings.fetch_read_timeout_s,
        write=settings.fetch_read_timeout_s,
        pool=settings.fetch_connect_timeout_s,
    )


async def fetch_image_bytes(image_url: str) -> bytes:
    try:
        async with httpx.AsyncClient(timeout=_timeout()) as client:
            resp = await client.get(image_url)
            resp.raise_for_status()
            return resp.content
    except httpx.HTTPError as e:
        raise FetchError(image_url, e) from e


async def fetch_stroke_document(stroke_url: str) -> StrokeDocument:
    try:
        async with httpx.AsyncClient(timeout=_timeout()) as client:
            resp = await client.get(stroke_url)
            resp.raise_for_status()
            return StrokeDocument.model_validate(resp.json())
    except httpx.HTTPError as e:
        raise FetchError(stroke_url, e) from e
