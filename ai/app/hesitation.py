"""
Stage 3: stroke 뭉치를 OCR 로 뽑은 글자와 정렬해서 hesitationPoints 를 만든다.

stroke_metrics.py 는 "이 글에서 총 몇 번, 얼마나 오래 멈췄는가"라는 집계만 안다 —
그 멈춤이 몇 번째 글자 앞이었는지는 모른다. 여기서는:

  1. stroke 들을 "한 글자를 쓰는 동안 그은 뭉치(클러스터)"로 묶는다
     (뭉치 사이 간격이 크면 다음 글자로 넘어간 것으로 본다)
  2. 뭉치 개수와 fullText 의 공백 아닌 글자 수가 정확히 같으리라는 보장은 없으므로,
     순서 "비율"로 대응시킨다(비례 정렬) — 아이가 왼쪽→오른쪽 순서로 쓴다는 가정만 있으면 된다
  3. 유난히 길게 멈췄거나(긴 pause) 유난히 여러 번 그은(retry) 뭉치를 hesitationPoints 로 뽑는다

한계: jamo 는 정확한 좌표 기반 분석 대신 그 글자의 초성으로 근사한다(예: "놀" → "ㄴ").
실제로 초성/중성/종성 중 어디서 멈췄는지 특정하려면 Hangul 블록 내 stroke 좌표까지
봐야 하는데, 이번 라운드에서는 범위 밖으로 뺀다.
"""

from __future__ import annotations

import statistics
from dataclasses import dataclass, field

from app.config import settings
from app.schemas import HesitationPoint, Stroke

_CHOSEONG = list("ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ")


@dataclass
class _CharCluster:
    strokes: list[Stroke] = field(default_factory=list)
    preceding_pause_ms: int = 0  # 이 뭉치 첫 stroke 전의 멈춤. 첫 뭉치는 0.


def _cluster_strokes(strokes: list[Stroke], intra_char_gap_ms: int) -> list[_CharCluster]:
    if not strokes:
        return []

    clusters = [_CharCluster(strokes=[strokes[0]])]
    for prev, cur in zip(strokes, strokes[1:]):
        gap = cur.pen_down_at - prev.pen_up_at
        if gap >= intra_char_gap_ms:
            clusters.append(_CharCluster(strokes=[cur], preceding_pause_ms=max(gap, 0)))
        else:
            clusters[-1].strokes.append(cur)
    return clusters


def _non_space_positions(full_text: str) -> list[int]:
    """공백이 아닌 글자들의, full_text 안에서의 실제 인덱스만 뽑는다."""
    return [i for i, ch in enumerate(full_text) if not ch.isspace()]


def _align_cluster_to_char_index(
    cluster_index: int, cluster_count: int, non_space_positions: list[int]
) -> int:
    """클러스터 순서를 공백 아닌 글자 순서에 비례해서 대응시킨다.

    클러스터 개수와 글자 수가 같으면 그냥 1:1 대응이 되고, 다르면 비율로 가장
    가까운 글자를 고른다 — 정확하진 않지만 "대략 몇 번째 글자 근처"는 맞힌다.
    """
    if cluster_count <= 1:
        return non_space_positions[0]
    ratio = cluster_index / (cluster_count - 1)
    target = round(ratio * (len(non_space_positions) - 1))
    return non_space_positions[target]


def _guess_choseong(char: str) -> str | None:
    code = ord(char) - 0xAC00
    if 0 <= code < 19 * 21 * 28:
        return _CHOSEONG[code // (21 * 28)]
    return None  # 한글 음절이 아님(숫자, 문장부호 등)


def compute_hesitation_points(
    strokes: list[Stroke],
    full_text: str,
    intra_char_gap_ms: int | None = None,
    hesitation_pause_ms: int | None = None,
) -> list[HesitationPoint]:
    intra_char_gap_ms = intra_char_gap_ms or settings.intra_char_gap_ms
    hesitation_pause_ms = hesitation_pause_ms or settings.hesitation_pause_ms

    clusters = _cluster_strokes(strokes, intra_char_gap_ms)
    non_space_positions = _non_space_positions(full_text)
    if not clusters or not non_space_positions:
        return []

    # "이 글에서 글자 하나 쓰는 데 보통 stroke 몇 개 썼나" — 이보다 훨씬 많이 그은
    # 클러스터는 다시 그리기(재시도)를 의심한다.
    typical_strokes_per_char = statistics.median(len(c.strokes) for c in clusters)

    points: list[HesitationPoint] = []
    for i, cluster in enumerate(clusters):
        retry_count = max(0, len(cluster.strokes) - round(typical_strokes_per_char))
        is_long_pause = cluster.preceding_pause_ms >= hesitation_pause_ms
        if not is_long_pause and retry_count == 0:
            continue  # 평범하게 쓴 글자는 보고하지 않는다

        char_index = _align_cluster_to_char_index(i, len(clusters), non_space_positions)
        char = full_text[char_index]
        dwell_ms = cluster.strokes[-1].pen_up_at - cluster.strokes[0].pen_down_at
        points.append(
            HesitationPoint(
                char_index=char_index,
                char=char,
                jamo=_guess_choseong(char),
                duration_ms=cluster.preceding_pause_ms + max(dwell_ms, 0),
                retry_count=retry_count,
            )
        )
    return points
