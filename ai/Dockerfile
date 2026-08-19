# 로컬 개발용 .venv 와 버전을 맞춘다(py -3.10).
FROM python:3.10-slim

ENV TZ=Asia/Seoul \
    PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

# 헬스체크용. 베이스 이미지에 기본 포함되지 않으므로 명시적으로 설치한다.
RUN apt-get update \
 && apt-get install -y --no-install-recommends curl \
 && rm -rf /var/lib/apt/lists/*

# root 로 실행하지 않는다.
RUN useradd --system --create-home --shell /usr/sbin/nologin writegrow

WORKDIR /app

# requirements 만 먼저 복사해서, 의존성이 안 바뀌면 이 레이어가 캐시되게 한다.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app app

USER writegrow
EXPOSE 8000

# OPENAI_API_KEY 등 비밀 값은 이미지에 넣지 않는다. 컨테이너를 띄우는 쪽
# (docker-compose 등)에서 환경 변수로 주입한다.

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=5 \
    CMD curl -fsS http://localhost:8000/health || exit 1

ENTRYPOINT ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
