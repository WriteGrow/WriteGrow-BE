from app.hesitation import compute_hesitation_points
from app.schemas import Stroke


def _build_strokes(hesitant_cluster_index: int, n_clusters: int = 12) -> list[Stroke]:
    """
    n_clusters 개의 "글자 뭉치"를 만든다. 뭉치는 보통 stroke 1개, 간격 500ms(클러스터
    경계로 인식되기엔 충분하지만 머뭇거림으로 보기엔 짧음)로 이어진다. 단 하나
    (hesitant_cluster_index 번째)만 stroke 3개 + 앞에 3000ms 의 긴 멈춤을 준다.
    """
    strokes: list[Stroke] = []
    t = 0
    idx = 0
    for cluster_i in range(n_clusters):
        if cluster_i > 0:
            t += 3000 if cluster_i == hesitant_cluster_index else 500
        stroke_count = 3 if cluster_i == hesitant_cluster_index else 1
        for _ in range(stroke_count):
            down, up = t, t + 150
            strokes.append(Stroke(index=idx, pen_down_at=down, pen_up_at=up, points=[]))
            idx += 1
            t = up + 50  # 같은 뭉치 안에서는 짧은 간격
    return strokes


def test_finds_the_one_hesitant_character_and_matches_contract_example():
    """docs/ai-contract.md 예시(글 "오늘 학교에서 친구랑 놀앗다", "놀"에서 머뭇거림)를 재현한다."""
    full_text = "오늘 학교에서 친구랑 놀앗다"
    non_space_char_count = sum(1 for ch in full_text if not ch.isspace())  # 12
    hesitant_index = 9  # 공백 뺀 글자 중 10번째 = "놀" (0,1=오늘 3,4,5,6=학교에서 8,9,10=친구랑 → 다음이 놀)
    strokes = _build_strokes(hesitant_cluster_index=hesitant_index, n_clusters=non_space_char_count)

    points = compute_hesitation_points(strokes, full_text)

    assert len(points) == 1
    point = points[0]
    assert point.char == "놀"
    assert point.char_index == full_text.index("놀")
    assert point.jamo == "ㄴ"  # "놀"의 초성 — 계약서 예시와 동일
    assert point.retry_count == 2  # stroke 3개 - 평범한 글자의 stroke 1개
    assert point.duration_ms > 3000  # 최소 preceding pause(3000ms) 이상


def test_no_hesitation_when_writing_is_uniform():
    full_text = "오늘 학교"  # 공백 제외 4글자
    strokes = _build_strokes(hesitant_cluster_index=-1, n_clusters=4)  # -1: 아무 뭉치도 안 튐

    points = compute_hesitation_points(strokes, full_text)

    assert points == []


def test_empty_strokes_or_text_return_no_points():
    assert compute_hesitation_points([], "오늘") == []
    assert compute_hesitation_points(_build_strokes(0, 3), "") == []


def _build_strokes_from_clusters(cluster_specs: list[tuple[int, int]]) -> list[Stroke]:
    """cluster_specs: (클러스터 stroke 개수, 그 클러스터 앞 pause ms) 목록.
    첫 클러스터의 pause 는 항상 0 취급된다. 클러스터 사이 간격이 지정한 pause 값과
    정확히 일치하도록, 마지막 stroke 뒤에는 여분의 간격을 남기지 않는다."""
    strokes: list[Stroke] = []
    t = 0
    idx = 0
    for i, (size, pause) in enumerate(cluster_specs):
        if i > 0:
            t += pause
        for j in range(size):
            if j > 0:
                t += 50  # 같은 클러스터 안에서의 간격
            down, up = t, t + 150
            strokes.append(Stroke(index=idx, pen_down_at=down, pen_up_at=up, points=[]))
            idx += 1
            t = up
    return strokes


def test_no_duplicate_or_missing_char_when_clusters_outnumber_characters():
    """실제 마우스 입력(노트북) 테스트로 재현된 버그의 회귀 테스트.

    글자 수(5)보다 클러스터 수(10)가 훨씬 많을 때, 예전 방식(클러스터마다
    가장 가까운 글자에 개별 반올림)은 서로 다른 클러스터가 같은 글자로
    몰려 중복 보고되고, 반대로 아무 클러스터도 안 걸린 글자는 조용히
    누락됐다. 이제는 글자 하나당 hesitationPoint 가 0개 또는 1개만 나와야 한다.
    """
    full_text = "가나다라마"  # 공백 없는 5글자
    cluster_sizes = [3, 1, 4, 1, 4, 1, 1, 1, 4, 4]
    pauses = [0, 487, 487, 827, 419, 503, 459, 1478, 450, 1448]
    strokes = _build_strokes_from_clusters(list(zip(cluster_sizes, pauses)))

    points = compute_hesitation_points(strokes, full_text)

    char_indices = [p.char_index for p in points]
    assert len(char_indices) == len(set(char_indices))  # 같은 글자가 두 번 나오지 않는다
    assert len(points) == 1
    assert points[0].char_index == 4  # "마" — 마지막 두 클러스터(4개+4개)가 몰린 글자
    assert points[0].retry_count == 3  # 글자당 중앙값(5) 대비 총 8 stroke → 3
