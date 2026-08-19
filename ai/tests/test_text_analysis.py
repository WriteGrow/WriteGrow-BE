from unittest.mock import AsyncMock, MagicMock, patch

from app.text_analysis import _locate, analyze_text


def _fake_response(errors: list[dict]):
    import json

    message = MagicMock()
    message.content = json.dumps({"errors": errors})
    choice = MagicMock()
    choice.message = message
    response = MagicMock()
    response.choices = [choice]
    return response


def test_locate_finds_first_unused_occurrence():
    text = "밥을 먹었다 밥을 또 먹었다"
    first = _locate(text, "밥을", set())
    assert first == (0, 2)

    # 이미 첫 등장을 썼으니, 두 번째 등장 위치를 찾아야 한다
    second = _locate(text, "밥을", {first})
    assert second == (text.index("밥을", first[1]), text.index("밥을", first[1]) + 2)


def test_locate_returns_none_when_not_found():
    assert _locate("오늘 학교", "없는말", set()) is None


@patch("app.text_analysis.AsyncOpenAI")
async def test_analyze_text_computes_indices_and_drops_unmatchable_original(mock_openai_cls):
    text = "사람이 만았다"
    mock_client = MagicMock()
    mock_client.chat.completions.create = AsyncMock(
        return_value=_fake_response(
            [
                {
                    "type": "FINAL_CONSONANT",
                    "original": "만았다",
                    "suggestion": "많았다",
                    "confidence": 0.9,
                    "reason": "받침 표기",
                },
                {
                    # 모델이 원문에 없는 문자열을 줬다면(환각) 위치를 못 찾으니 버려야 한다
                    "type": "SPELLING",
                    "original": "전혀다른말",
                    "suggestion": "x",
                    "confidence": 0.9,
                    "reason": "x",
                },
            ]
        )
    )
    mock_openai_cls.return_value = mock_client

    errors = await analyze_text(text, topic=None)

    assert len(errors) == 1
    assert errors[0].type == "FINAL_CONSONANT"
    assert errors[0].start_index == text.index("만았다")
    assert errors[0].end_index == text.index("만았다") + len("만았다")
    assert errors[0].suggestion == "많았다"


@patch("app.text_analysis.AsyncOpenAI")
async def test_analyze_text_returns_empty_list_when_no_errors(mock_openai_cls):
    mock_client = MagicMock()
    mock_client.chat.completions.create = AsyncMock(return_value=_fake_response([]))
    mock_openai_cls.return_value = mock_client

    errors = await analyze_text("오늘 학교에 갔다", topic=None)

    assert errors == []
