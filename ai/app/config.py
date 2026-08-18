"""환경 변수로 조절하는 설정값들. 기본값만으로도 로컬에서 바로 돌아간다."""

from __future__ import annotations

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_prefix="WRITEGROW_AI_")

    # stroke_metrics.py: 이보다 짧은 pen-up 간격은 "자연스러운 이어쓰기"로 보고 무시한다.
    pause_threshold_ms: int = 600

    # imageUrl / strokeUrl 다운로드용 타임아웃. presigned URL 은 이미 S3 를 가리키므로
    # 백엔드→AI 타임아웃(3s/60s, docs/ai-contract.md)과는 별개로 우리가 정한다.
    fetch_connect_timeout_s: float = 5.0
    fetch_read_timeout_s: float = 20.0


settings = Settings()
