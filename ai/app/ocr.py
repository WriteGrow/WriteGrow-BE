"""
Stage 4: 손글씨 이미지를 텍스트로 바꾼다 (비전 LLM 호출, GPT-4o).

핵심 아이디어: 계약서는 "자신 없는 구절은 낮은 confidence 를 정직하게 내려 달라"고
요구한다. 모델에게 "이거 얼마나 확신해?"라고 다시 물어보면(자기평가) 그럴듯하게
지어낼 위험이 있으므로, 그 대신 모델이 글자를 예측할 때 실제로 가진 확률값
(logprobs)을 그대로 가져와 confidence 로 쓴다.

문제는 GPT 토크나이저가 한글을 바이트 단위로 쪼개서, 토큰 경계가 글자 경계와
안 맞을 수 있다는 것이다(한글 한 글자 = UTF-8 3바이트, 토큰 하나는 그중 일부만
담을 수 있음). 그래서 `_tokens_to_chars` 가 바이트를 순서대로 다시 이어붙이며
글자를 복원하고, 그 글자를 만드는 데 관여한 토큰들의 logprob 을 같이 옮긴다.
"""

from __future__ import annotations

import base64
import codecs
import math
import re
from dataclasses import dataclass, field

from openai import AsyncOpenAI, OpenAIError

from app.config import settings
from app.schemas import Segment

SYSTEM_PROMPT = (
    "너는 초등 저학년 아이가 태블릿에 손글씨로 쓴 한국어 문장을 그대로 옮겨 적는 필사자다.\n"
    "이미지에 보이는 글자를 아이가 실제로 쓴 그대로 옮겨라. 맞춤법이 틀렸어도 표준 맞춤법으로\n"
    "고치지 마라 (예: 아이가 '만이'라고 썼으면 '많이'로 고치지 말고 '만이'라고 그대로 옮길 것).\n"
    "설명, 인사말, 따옴표, 마크다운 없이 옮겨 적은 문장 텍스트만 출력해라."
)


class OcrError(Exception):
    """OCR 호출 실패(네트워크, 인증, rate limit 등). main.py 가 잡아서 안전하게 처리한다."""


@dataclass
class OcrResult:
    full_text: str
    overall_confidence: float | None
    segments: list[Segment] = field(default_factory=list)


def _image_data_url(image_bytes: bytes, mime: str = "image/png") -> str:
    b64 = base64.b64encode(image_bytes).decode("ascii")
    return f"data:{mime};base64,{b64}"


@dataclass
class _CharProb:
    char: str
    logprob: float


def _tokens_to_chars(logprob_content) -> list[_CharProb]:
    """
    바이트 단위 토큰을 순서대로 이어붙여 완성된 글자로 복원하면서 logprob 을 같이 옮긴다.

    - 한 글자가 여러 토큰에 걸쳐 나오면: 관여한 토큰들의 logprob 을 더한다
      (= 그 바이트들을 순서대로 낼 확률을 곱한다는 뜻과 같다, log 공간에서는 덧셈).
    - 한 토큰이 여러 글자를 한 번에 내면: 토큰의 logprob 을 글자 수만큼 균등하게 나눈다
      (정확하진 않지만, 구간 평균 confidence 를 낼 때는 충분한 근사다).
    """
    decoder = codecs.getincrementaldecoder("utf-8")()
    chars: list[_CharProb] = []
    pending_logprob = 0.0

    for token in logprob_content:
        token_bytes = bytes(token.bytes) if token.bytes else token.token.encode("utf-8")
        pending_logprob += token.logprob
        decoded = decoder.decode(token_bytes)
        if decoded:
            share = pending_logprob / len(decoded)
            chars.extend(_CharProb(char=ch, logprob=share) for ch in decoded)
            pending_logprob = 0.0
        # decoded 가 비어 있으면 아직 글자가 안 완성된 것 — 누적된 logprob 은
        # 다음 토큰으로 이월된다(pending_logprob 을 리셋하지 않음).

    return chars


def _segments_from_chars(full_text: str, chars: list[_CharProb]) -> list[Segment]:
    """계약서 예시(segments: "오늘", "학교에서", ...)와 같은 단위로, 공백/줄바꿈 기준으로 나눈다.

    \\S+ 로 "공백이 아닌 연속 구간"을 직접 찾으므로 스페이스든 줄바꿈이든 몇 개가 연달아
    있든 상관없이 단어 경계를 정확히 잡는다(실제 다중 공백 텍스트로 테스트하다 발견한
    문제 — 예전엔 " " 하나만 구분자로 봐서 "\\n저희" 처럼 줄바꿈이 낀 구절이 뭉쳐 나왔다).
    """
    segments: list[Segment] = []
    for match in re.finditer(r"\S+", full_text):
        start, end = match.start(), match.end()
        word_chars = chars[start:end]
        confidence = (
            math.exp(sum(c.logprob for c in word_chars) / len(word_chars))
            if word_chars
            else None
        )
        segments.append(
            Segment(text=match.group(), confidence=confidence, start_index=start, end_index=end)
        )
    return segments


async def recognize(image_bytes: bytes, expected_topic: str | None) -> OcrResult:
    prompt = "이 손글씨를 옮겨 적어줘."
    if expected_topic:
        prompt += f" (아이가 고른 글쓰기 주제: {expected_topic})"

    try:
        client = AsyncOpenAI()  # OPENAI_API_KEY 환경 변수를 SDK가 알아서 읽는다
        response = await client.chat.completions.create(
            model=settings.ocr_model,
            temperature=0,
            logprobs=True,
            messages=[
                {"role": "system", "content": SYSTEM_PROMPT},
                {
                    "role": "user",
                    "content": [
                        {"type": "text", "text": prompt},
                        {"type": "image_url", "image_url": {"url": _image_data_url(image_bytes)}},
                    ],
                },
            ],
        )
    except OpenAIError as e:
        raise OcrError(f"OCR 호출 실패: {e!r}") from e

    choice = response.choices[0]
    raw_text = choice.message.content or ""
    full_text = raw_text.strip()
    if not full_text:
        return OcrResult(full_text="", overall_confidence=None, segments=[])

    # strip() 으로 앞뒤 공백을 잘라낸 만큼, 글자별 logprob 목록도 같이 맞춰준다
    # (안 그러면 segments 의 startIndex/endIndex 가 full_text 랑 어긋난다).
    leading = len(raw_text) - len(raw_text.lstrip())
    all_chars = _tokens_to_chars(choice.logprobs.content) if choice.logprobs else []
    chars = all_chars[leading : leading + len(full_text)]

    overall_confidence = (
        math.exp(sum(c.logprob for c in chars) / len(chars)) if chars else None
    )
    segments = _segments_from_chars(full_text, chars)

    return OcrResult(full_text=full_text, overall_confidence=overall_confidence, segments=segments)
