from app.schemas import Stroke, StrokeDocument
from app.stroke_metrics import compute_process_metric


def _stroke(index: int, down: int, up: int) -> Stroke:
    return Stroke(index=index, pen_down_at=down, pen_up_at=up, points=[])


def test_pause_count_ignores_short_gaps_between_strokes():
    """
    stroke0: 0~200
    stroke1: 200~450      (직전과 간격 0 → 자연스러운 이어쓰기)
    stroke2: 1200~1450    (직전과 간격 750ms → 멈춤으로 셀 것)
    stroke3: 1500~1700    (직전과 간격 50ms  → 너무 짧아 무시)
    """
    doc = StrokeDocument(
        writing_id=1,
        strokes=[
            _stroke(0, 0, 200),
            _stroke(1, 200, 450),
            _stroke(2, 1200, 1450),
            _stroke(3, 1500, 1700),
        ],
    )

    metric, pauses = compute_process_metric(doc, pause_threshold_ms=600)

    assert metric.total_duration_ms == 1700  # totalDurationMs 없으면 strokes 로 역산
    assert metric.pause_count == 1
    assert metric.longest_pause_ms == 750
    assert metric.avg_stroke_duration_ms == 225  # (200+250+250+200)/4
    assert metric.hesitation_points == []  # 이 단계에서는 채우지 않는다

    # pauses 는 다음 단계(hesitation.py)가 그대로 재사용할 원자료이므로
    # 임계값 미만인 것도 걸러지지 않고 다 들어있어야 한다.
    assert [p.gap_ms for p in pauses] == [750, 50]


def test_prefers_explicit_total_duration_from_document():
    """strokeUrl 문서가 totalDurationMs 를 이미 갖고 있으면 그 값을 신뢰한다."""
    doc = StrokeDocument(
        writing_id=1,
        total_duration_ms=92000,
        strokes=[_stroke(0, 0, 100)],
    )

    metric, _ = compute_process_metric(doc)

    assert metric.total_duration_ms == 92000


def test_empty_strokes_do_not_crash():
    doc = StrokeDocument(writing_id=1, strokes=[])

    metric, pauses = compute_process_metric(doc)

    assert metric.total_duration_ms == 0
    assert metric.pause_count == 0
    assert metric.longest_pause_ms == 0
    assert metric.avg_stroke_duration_ms == 0
    assert pauses == []
