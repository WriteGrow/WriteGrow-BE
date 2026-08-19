"""환경 변수로 조절하는 설정값들. 기본값만으로도 로컬에서 바로 돌아간다."""

from __future__ import annotations

from dotenv import load_dotenv
from pydantic_settings import BaseSettings, SettingsConfigDict

# ai/.env 가 있으면 읽어서 프로세스 환경 변수로 얹는다(OPENAI_API_KEY 등).
# .env 는 .gitignore 에 있으니 실수로 커밋될 걱정은 없다.
load_dotenv()


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="WRITEGROW_AI_")

    # stroke_metrics.py: 이보다 짧은 pen-up 간격은 "자연스러운 이어쓰기"로 보고 무시한다.
    pause_threshold_ms: int = 600

    # hesitation.py: 이보다 짧은 간격은 "같은 글자를 계속 쓰는 중"으로 본다(클러스터 경계 기준).
    intra_char_gap_ms: int = 400
    # hesitation.py: 클러스터 앞 멈춤이 이보다 길어야 hesitationPoints 에 보고한다.
    # pause_threshold_ms 보다 기준을 높게 잡아, "집계에는 잡히지만 보고할 정도는 아닌" 멈춤을 거른다.
    hesitation_pause_ms: int = 1500

    # imageUrl / strokeUrl 다운로드용 타임아웃. presigned URL 은 이미 S3 를 가리키므로
    # 백엔드→AI 타임아웃(3s/60s, docs/ai-contract.md)과는 별개로 우리가 정한다.
    fetch_connect_timeout_s: float = 5.0
    fetch_read_timeout_s: float = 20.0

    # OCR(ocr.py) 에 쓸 모델. API 키 자체는 여기 두지 않는다 — ai/.env 의
    # OPENAI_API_KEY 를 OpenAI SDK가 알아서 읽는다(위 load_dotenv 가 .env 를 얹어준 덕분).
    ocr_model: str = "gpt-4o"

    # text_analysis.py(오류 분석)에 쓸 모델. 이미지가 없어 비전 모델일 필요는 없지만,
    # "이게 아이다운 표현인가 오류인가"를 판단하는 미묘한 작업이라 일단 OCR과 같은
    # 모델로 시작한다.
    error_analysis_model: str = "gpt-4o"


settings = Settings()
