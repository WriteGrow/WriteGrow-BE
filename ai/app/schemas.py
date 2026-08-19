"""
백엔드 ↔ AI 서버 계약(docs/ai-contract.md)을 그대로 옮긴 pydantic 모델.

필드 이름은 백엔드가 보내는/기대하는 JSON 키와 1:1로 맞춘다(camelCase 유지).
계약서 원칙대로 "AI 쪽이 필드를 늘려도 백엔드는 안 깨진다"는 방향이므로,
여기서도 우리가 필드를 추가하는 건 자유롭지만 계약서에 있는 필드는 이름/타입을 지킨다.
"""

from __future__ import annotations

from typing import Optional

from pydantic import BaseModel, ConfigDict, Field


# ── 백엔드 → AI 요청 ──────────────────────────────────────────────

class Canvas(BaseModel):
    width: Optional[int] = None
    height: Optional[int] = None


class AnalyzeRequest(BaseModel):
    writing_id: int = Field(alias="writingId")
    image_url: str = Field(alias="imageUrl")
    stroke_url: str = Field(alias="strokeUrl")
    canvas: Optional[Canvas] = None
    expected_topic: Optional[str] = Field(default=None, alias="expectedTopic")

    model_config = ConfigDict(populate_by_name=True)


# ── strokeUrl 이 가리키는 문서 (획 데이터) ──────────────────────────

class Point(BaseModel):
    x: float
    y: float
    t: int
    pressure: Optional[float] = None


class Stroke(BaseModel):
    index: int
    pen_down_at: int = Field(alias="penDownAt")
    pen_up_at: int = Field(alias="penUpAt")
    points: list[Point] = Field(default_factory=list)

    model_config = ConfigDict(populate_by_name=True)


class StrokeDocument(BaseModel):
    writing_id: int = Field(alias="writingId")
    canvas_width: Optional[int] = Field(default=None, alias="canvasWidth")
    canvas_height: Optional[int] = Field(default=None, alias="canvasHeight")
    stroke_count: Optional[int] = Field(default=None, alias="strokeCount")
    total_duration_ms: Optional[int] = Field(default=None, alias="totalDurationMs")
    strokes: list[Stroke] = Field(default_factory=list)

    model_config = ConfigDict(populate_by_name=True)


# ── AI → 백엔드 응답 ──────────────────────────────────────────────

class Segment(BaseModel):
    text: str
    confidence: float
    start_index: int = Field(serialization_alias="startIndex")
    end_index: int = Field(serialization_alias="endIndex")


class HesitationPoint(BaseModel):
    char_index: int = Field(serialization_alias="charIndex")
    # 계약서 wire 포맷은 "char". 자바 쪽에서 예약어라 character 로 매핑하는 것뿐이니
    # 우리는 그냥 char 로 내보내면 된다.
    char: str
    jamo: Optional[str] = None
    duration_ms: int = Field(serialization_alias="durationMs")
    retry_count: int = Field(serialization_alias="retryCount")


class ProcessMetric(BaseModel):
    total_duration_ms: int = Field(serialization_alias="totalDurationMs")
    pause_count: int = Field(serialization_alias="pauseCount")
    longest_pause_ms: int = Field(serialization_alias="longestPauseMs")
    avg_stroke_duration_ms: int = Field(serialization_alias="avgStrokeDurationMs")
    hesitation_points: list[HesitationPoint] = Field(
        default_factory=list, serialization_alias="hesitationPoints"
    )


class TextAnalyzeRequest(BaseModel):
    writing_id: int = Field(alias="writingId")
    text: str
    topic: Optional[str] = None

    model_config = ConfigDict(populate_by_name=True)


class ErrorItem(BaseModel):
    type: str
    start_index: int = Field(serialization_alias="startIndex")
    end_index: int = Field(serialization_alias="endIndex")
    original: str
    suggestion: str
    confidence: float
    reason: Optional[str] = None


class TextAnalyzeResponse(BaseModel):
    errors: list[ErrorItem] = Field(default_factory=list)


class AnalyzeResponse(BaseModel):
    full_text: str = Field(serialization_alias="fullText")
    overall_confidence: Optional[float] = Field(
        default=None, serialization_alias="overallConfidence"
    )
    segments: list[Segment] = Field(default_factory=list)
    process_metric: Optional[ProcessMetric] = Field(
        default=None, serialization_alias="processMetric"
    )
