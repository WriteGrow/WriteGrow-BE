"""
POST /text/analyze (REQ-03): 확정된 텍스트에서 맞춤법·띄어쓰기 등 오류를 찾는다.

OCR(ocr.py)과 confidence 의 성격이 다르다. OCR 은 "이 글자를 정확히 읽었나"라는
지각 문제라 logprobs(모델이 실제로 가진 확률)를 그대로 쓸 수 있었다. 여긴
"이 표현이 교정이 필요한가"라는 판단 문제라 그런 확률이 없다 — 그래서 confidence 는
모델이 스스로 매긴 값을 쓰되, 프롬프트에 계약서의 원칙("애매하면 낮게, 아이가 맞게 쓴
표현을 틀렸다고 배우게 하지 마라")을 그대로 박아 넣는다.

startIndex/endIndex 는 모델에게 세게 하지 않는다. LLM 은 글자 수 세기(특히 한글처럼
자모가 얽힌 문자)에 약하다. 대신 모델에게는 "원문에서 그대로 복사한 부분(original)"만
받고, 그 문자열이 text 안 어디에 있는지는 우리가 직접(str.find) 찾는다 — 모델이
못 미더운 일은 시키지 않고, 우리가 더 잘할 수 있는 건 코드로 한다는 원칙을 OCR 때와
동일하게 적용한 것이다.
"""

from __future__ import annotations

import json
from dataclasses import dataclass

from openai import AsyncOpenAI, OpenAIError

from app.config import settings
from app.schemas import ErrorItem

ERROR_TYPES = [
    "SPELLING",
    "SPACING",
    "FINAL_CONSONANT",
    "PARTICLE_ENDING",
    "SENTENCE_STRUCTURE",
    "VOCABULARY",
]

SYSTEM_PROMPT = f"""너는 초등 저학년(1~3학년) 아이가 쓴 글을 검토하는 문해력 코치다.
아이가 쓴 텍스트에서 다음 6가지 유형의 오류만 찾는다: {", ".join(ERROR_TYPES)}.

절대 지켜야 할 원칙:
1. 아이다운 자연스러운 구어체 표현("되게 귀엽고", "집에왔다" 등)은 문맥상 자연스러우면
   오류로 보지 않는다. 표준 문어체가 아니라는 이유만으로 지적하지 마라.
2. confidence 는 정확도 점수가 아니라 "이 지적을 아이에게 보여줘도 되는가"를 정하는
   스위치다. 애매하면 반드시 낮은 값(0.5 이하)을 줘라. 틀렸는지 확신이 안 서는데
   높은 값을 주면, 아이가 맞게 쓴 표현을 틀렸다고 배우게 되는 최악의 결과가 된다.
3. original 필드는 아이가 실제로 쓴 텍스트에서 한 글자도 틀리지 않고 그대로
   복사해서 채워라 — 요약하거나 다듬지 마라. 이 필드로 텍스트 내 위치를 찾는다.
4. 오류가 없으면 빈 배열을 반환해라. 그것도 정상이다. 억지로 오류를 만들어내지 마라."""

_RESPONSE_SCHEMA = {
    "type": "object",
    "properties": {
        "errors": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "type": {"type": "string", "enum": ERROR_TYPES},
                    "original": {"type": "string"},
                    "suggestion": {"type": "string"},
                    "confidence": {"type": "number"},
                    "reason": {"type": "string"},
                },
                "required": ["type", "original", "suggestion", "confidence", "reason"],
                "additionalProperties": False,
            },
        }
    },
    "required": ["errors"],
    "additionalProperties": False,
}


class TextAnalysisError(Exception):
    """오류 분석 호출 실패. main.py 가 잡아서 502 로 넘긴다.

    빈 errors 배열은 '오류 없음'이라는 정상 응답이라(계약서 명시), OCR 처럼
    실패를 빈 값으로 흉내내면 안 된다 — 실패는 실패로 알려야 백엔드가
    ANALYSIS_FAILED 로 정확히 기록한다.
    """


@dataclass
class _Candidate:
    type: str
    original: str
    suggestion: str
    confidence: float
    reason: str | None


def _locate(text: str, original: str, used_spans: set[tuple[int, int]]) -> tuple[int, int] | None:
    """text 안에서 original 이 나타나는, 아직 다른 오류가 안 쓴 위치를 찾는다.

    같은 문자열이 여러 번 나올 수 있어(예: 같은 단어 반복 오류) 이미 쓰인 구간은
    건너뛴다. 아예 못 찾으면(모델이 원문과 다르게 옮겨 적음) None — 위치를 못
    믿으니 그 후보는 버린다.
    """
    start = 0
    while True:
        idx = text.find(original, start)
        if idx == -1:
            return None
        span = (idx, idx + len(original))
        if span not in used_spans:
            return span
        start = idx + 1


async def analyze_text(text: str, topic: str | None) -> list[ErrorItem]:
    user_prompt = f"아이가 쓴 글: {text!r}"
    if topic:
        user_prompt += f"\n글쓰기 주제: {topic}"

    try:
        client = AsyncOpenAI()
        response = await client.chat.completions.create(
            model=settings.error_analysis_model,
            temperature=0,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {"role": "user", "content": user_prompt},
            ],
            response_format={
                "type": "json_schema",
                "json_schema": {"name": "error_list", "schema": _RESPONSE_SCHEMA, "strict": True},
            },
        )
        raw = json.loads(response.choices[0].message.content or "{}")
    except (OpenAIError, json.JSONDecodeError) as e:
        raise TextAnalysisError(f"오류 분석 호출 실패: {e!r}") from e

    items: list[ErrorItem] = []
    used_spans: set[tuple[int, int]] = set()
    for raw_candidate in raw.get("errors", []):
        candidate = _Candidate(**raw_candidate)
        span = _locate(text, candidate.original, used_spans)
        if span is None:
            continue
        used_spans.add(span)
        items.append(
            ErrorItem(
                type=candidate.type,
                start_index=span[0],
                end_index=span[1],
                original=candidate.original,
                suggestion=candidate.suggestion,
                confidence=candidate.confidence,
                reason=candidate.reason,
            )
        )
    return items
