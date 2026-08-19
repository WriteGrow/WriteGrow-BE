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


def _bin_clusters_by_char(
    clusters: list[_CharCluster], non_space_positions: list[int]
) -> list[list[_CharCluster]]:
    """N개 클러스터를 M개 글자에 순서를 지키며 겹치지 않게 나눠 담는다.

    클러스터 개수와 글자 수가 다른 게 보통이다(특히 마우스/서투른 손글씨는 클러스터가
    잘게 쪼개지기 쉽다). 클러스터마다 "가장 가까운 글자"를 각자 반올림해서 고르면
    서로 다른 클러스터가 같은 글자로 몰려 중복 보고되거나, 반대로 아무 클러스터도
    안 걸린 글자가 조용히 누락되는 문제가 생긴다(실제 마우스 입력 테스트로 발견).

    그 대신 클러스터를 순서대로 M개 구간으로 나눠 담아서, 한 글자에 여러 클러스터가
    몰리는 건 허용하되 그 글자에 대한 판정은 한 번만 내리게 한다. N==M 이면 그냥
    1:1 대응이 된다.
    """
    m = len(non_space_positions)
    n = len(clusters)
    bins: list[list[_CharCluster]] = [[] for _ in range(m)]
    for i, cluster in enumerate(clusters):
        bin_index = min(m - 1, i * m // n)
        bins[bin_index].append(cluster)
    return bins


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

    bins = _bin_clusters_by_char(clusters, non_space_positions)

    # "이 글자 하나 쓰는 데 보통 stroke 몇 개 썼나" — 글자당(=bin당) 총 stroke 수의
    # 중앙값과 비교한다. 클러스터 단위가 아니라 글자 단위로 봐야, 한 글자가 클러스터
    # 여러 개로 쪼개진 경우에도 "이 글자를 쓰는 데 유난히 많이 그었다"를 올바르게 잰다.
    strokes_per_char = [sum(len(c.strokes) for c in b) for b in bins if b]
    if not strokes_per_char:
        return []
    typical_strokes_per_char = statistics.median(strokes_per_char)

    points: list[HesitationPoint] = []
    for char_index, char_clusters in zip(non_space_positions, bins):
        if not char_clusters:
            continue  # 이 글자에 배정된 클러스터가 없다 — 판단할 근거가 없으니 건너뛴다

        total_strokes = sum(len(c.strokes) for c in char_clusters)
        retry_count = max(0, total_strokes - round(typical_strokes_per_char))

        # 이 글자를 쓰기 시작하기 전 멈춤 + 이 글자를 쓰는 도중(클러스터 사이) 멈춤들.
        preceding_pause_ms = char_clusters[0].preceding_pause_ms
        internal_pause_ms = sum(c.preceding_pause_ms for c in char_clusters[1:])
        is_long_pause = max(preceding_pause_ms, internal_pause_ms) >= hesitation_pause_ms

        if not is_long_pause and retry_count == 0:
            continue  # 평범하게 쓴 글자는 보고하지 않는다

        char = full_text[char_index]
        dwell_ms = char_clusters[-1].strokes[-1].pen_up_at - char_clusters[0].strokes[0].pen_down_at
        points.append(
            HesitationPoint(
                char_index=char_index,
                char=char,
                jamo=_guess_choseong(char),
                duration_ms=preceding_pause_ms + max(dwell_ms, 0),
                retry_count=retry_count,
            )
        )
    return points
