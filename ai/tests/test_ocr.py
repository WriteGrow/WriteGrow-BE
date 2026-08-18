import math
from types import SimpleNamespace

import pytest

from app.ocr import _segments_from_chars, _tokens_to_chars


def _fake_token(text_bytes: bytes, logprob: float):
    """OpenAI 응답의 logprobs.content 항목 하나를 흉내낸다."""
    return SimpleNamespace(token="", logprob=logprob, bytes=list(text_bytes))


def test_char_split_across_two_tokens_keeps_full_char_and_sums_logprob():
    """한글 한 글자(UTF-8 3바이트)가 토큰 경계에 걸쳐 두 조각으로 나온 경우."""
    raw = "오".encode("utf-8")
    tokens = [
        _fake_token(raw[:1], -0.1),
        _fake_token(raw[1:], -0.2),
    ]

    chars = _tokens_to_chars(tokens)

    assert [c.char for c in chars] == ["오"]
    assert chars[0].logprob == pytest.approx(-0.3)


def test_one_token_producing_two_chars_splits_logprob_evenly():
    tokens = [_fake_token("가나".encode("utf-8"), -0.6)]

    chars = _tokens_to_chars(tokens)

    assert [c.char for c in chars] == ["가", "나"]
    assert chars[0].logprob == pytest.approx(-0.3)
    assert chars[1].logprob == pytest.approx(-0.3)


def test_segments_split_by_space_and_average_confidence_over_span():
    full_text = "오늘 학교"
    tokens = [_fake_token(full_text.encode("utf-8"), -0.4)]  # 글자 5개(공백 포함)에 고르게 분배
    chars = _tokens_to_chars(tokens)

    segments = _segments_from_chars(full_text, chars)

    assert [s.text for s in segments] == ["오늘", "학교"]
    assert segments[0].start_index == 0
    assert segments[0].end_index == 2
    assert segments[1].start_index == 3
    assert segments[1].end_index == 5
    # 토큰 하나(-0.4)가 글자 5개(오,늘,공백,학,교)에 균등 분배되므로 글자당 -0.08
    assert segments[0].confidence == pytest.approx(math.exp(-0.08))


def test_segments_split_on_newline_too():
    """실제 OCR 결과에서 줄바꿈이 낀 구절이 하나로 뭉치던 버그의 회귀 테스트."""
    full_text = "안녕하세요\n감사합니다"
    tokens = [_fake_token(full_text.encode("utf-8"), -0.3)]
    chars = _tokens_to_chars(tokens)

    segments = _segments_from_chars(full_text, chars)

    assert [s.text for s in segments] == ["안녕하세요", "감사합니다"]
