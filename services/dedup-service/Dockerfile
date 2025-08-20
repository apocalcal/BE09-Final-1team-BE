# Python 3.11 기반 이미지
FROM python:3.11-slim

# 작업 디렉토리 설정
WORKDIR /app

# 시스템 패키지 업데이트 및 필수 도구 설치
RUN apt-get update && apt-get install -y \
    gcc \
    g++ \
    curl \
    default-jdk \
    && rm -rf /var/lib/apt/lists/*

# Python 의존성 파일 복사 및 설치
COPY requirements.txt .
RUN pip install --no-cache-dir --upgrade pip setuptools wheel
RUN pip install --no-cache-dir -r requirements.txt

# KoNLPy 데이터 다운로드 (한번만 실행)
RUN python -c "import nltk; nltk.download('punkt')" || true

# 전체 프로젝트 복사 (app/ 디렉토리 포함)
COPY . .

# 환경 변수 설정
ENV PYTHONPATH=/app
ENV ENVIRONMENT=docker

# 포트 노출
EXPOSE 8084

# 헬스체크 추가
HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD curl -f http://localhost:8084/health || exit 1

# 애플리케이션 실행 (로컬 환경과 동일한 방식)
CMD ["python", "-m", "uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8084"]
