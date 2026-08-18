"""
Stage 2: 획(stroke) 시계열에서 "과정 지표"를 계산한다.

여기엔 AI/모델이 전혀 없다. stroke 하나하나가 pen-down(누른 시각)과
pen-up(뗀 시각)을 갖고 있으므로, 연속된 두 stroke 사이의 간격(= 펜을 떼고 있던 시간)을
계산하면 그게 곧 "멈춘 시간"이다. 다음 단계(hesitation.py)에서 이 멈춤들 중
유난히 긴 것을 골라 어떤 글자에서 멈췄는지와 연결한다.
"""

from __future__ import annotations

from dataclasses import dataclass

from app.schemas import ProcessMetric, Stroke, StrokeDocument


@dataclass(frozen=True)
class PauseEvent:
    """strokes[stroke_index] 를 긋기 직전에 있었던 멈춤 한 건."""

    stroke_index: int
    gap_ms: int


def compute_pauses(strokes: list[Stroke]) -> list[PauseEvent]:
    """연속된 두 stroke 사이, 펜이 떨어져 있던 시간(ms)을 전부 뽑는다.

    stroke 는 이미 index 오름차순(=실제로 그은 순서)으로 정렬되어 온다는 것이
    계약서에 명시되어 있으므로 그대로 순회하면 된다.
    """
    pauses: list[PauseEvent] = []
    for i in range(1, len(strokes)):
        prev_up = strokes[i - 1].pen_up_at
        cur_down = strokes[i].pen_down_at
        gap = cur_down - prev_up
        if gap > 0:
            pauses.append(PauseEvent(stroke_index=i, gap_ms=gap))
    return pauses


def compute_process_metric(
    doc: StrokeDocument,
    pause_threshold_ms: int = 600,
) -> tuple[ProcessMetric, list[PauseEvent]]:
    """processMetric 의 숫자 필드들 + (다음 단계에서 쓸) 전체 pause 목록을 함께 돌려준다.

    hesitationPoints 는 여기서 채우지 않는다(빈 리스트) — 그건 stroke 클러스터를
    OCR 텍스트와 정렬해야 하는 별도 단계(hesitation.py)의 몫이다.

    Args:
        pause_threshold_ms: 이보다 짧은 멈춤은 "그냥 다음 글자로 넘어가는 자연스러운
            간격"으로 보고 pauseCount 에 세지 않는다. 아이마다/기기마다 다를 수 있어
            설정값으로 뺐다.
    """
    strokes = doc.strokes
    pauses = compute_pauses(strokes)

    if doc.total_duration_ms is not None:
        total_duration_ms = doc.total_duration_ms
    elif strokes:
        total_duration_ms = strokes[-1].pen_up_at - strokes[0].pen_down_at
    else:
        total_duration_ms = 0

    counted_pauses = [p for p in pauses if p.gap_ms >= pause_threshold_ms]
    pause_count = len(counted_pauses)
    longest_pause_ms = max((p.gap_ms for p in pauses), default=0)

    if strokes:
        avg_stroke_duration_ms = round(
            sum(s.pen_up_at - s.pen_down_at for s in strokes) / len(strokes)
        )
    else:
        avg_stroke_duration_ms = 0

    metric = ProcessMetric(
        total_duration_ms=total_duration_ms,
        pause_count=pause_count,
        longest_pause_ms=longest_pause_ms,
        avg_stroke_duration_ms=avg_stroke_duration_ms,
        hesitation_points=[],
    )
    return metric, pauses
