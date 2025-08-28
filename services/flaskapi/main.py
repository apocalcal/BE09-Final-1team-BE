# services/flaskapi/main.py
# 로컬/도커 겸용 Flask 진입점
# - .env를 자동 로드
# - sqlite 경로(.data/flaskapi.sqlite3) 자동 보정
# - /health, /summary 엔드포인트 제공
# - 포트: 기본 7001 (ENV PORT로 변경)

import os
import re
import argparse
from pathlib import Path
from typing import List

from flask import Flask, request, jsonify
from flask_cors import CORS

# --- .env 로드 (없어도 통과) ---
try:
    from dotenv import load_dotenv
    load_dotenv()
except Exception:
    pass

# --- 경로/환경 기본값 ---
BASE_DIR = Path(__file__).resolve().parent
DATA_DIR = BASE_DIR / ".data"
DATA_DIR.mkdir(parents=True, exist_ok=True)  # sqlite 등 저장 폴더 보장

DEFAULT_SQLITE = f"sqlite:///{(DATA_DIR / 'flaskapi.sqlite3').as_posix()}"
DATABASE_URL = os.getenv("DATABASE_URL", DEFAULT_SQLITE)
APP_ENV = os.getenv("APP_ENV", "local")
PORT = int(os.getenv("PORT", "7001"))

# --- 유틸 ---
def running_in_docker() -> bool:
    return os.path.exists("/.dockerenv") or os.getenv("RUN_IN_DOCKER") == "1"

def _split_sentences(text: str) -> List[str]:
    # 한국어/영문 단순 문장 분리기 (구둣점 기준)
    text = re.sub(r"[\r\n]+", " ", text).strip()
    # 마침표/물음표/느낌표/종결부호 뒤에서 분리
    parts = re.split(r"(?<=[\.!\?]|[다요죠음임니까]\s*)\s+", text)
    # 깨끗하게 정리
    return [p.strip(" .!?\u3002\uFF01\uFF1F") for p in parts if p and p.strip()]

# --- 앱 팩토리 ---
def create_app() -> Flask:
    app = Flask(__name__)
    CORS(app)

    # DB URL을 앱 설정에 심어둠(프로젝트에 SQLAlchemy를 쓰는 경우를 대비)
    app.config["SQLALCHEMY_DATABASE_URI"] = DATABASE_URL
    app.config["APP_ENV"] = APP_ENV
    app.config["RUNNING_IN_DOCKER"] = running_in_docker()

    @app.get("/")
    def root():
        return {
            "ok": True,
            "msg": "flaskapi alive",
            "env": app.config["APP_ENV"],
            "docker": app.config["RUNNING_IN_DOCKER"],
        }, 200

    @app.get("/health")
    def health():
        # 필요 시 DB 접속 점검 로직 추가 가능
        return {"status": "UP"}, 200

    @app.post("/summary")
    def summary():
        """
        요청 예시:
        {
          "text": "요약할 본문",
          "lines": 3,
          "type": "DEFAULT",        # 선택
          "promptOverride": "..."   # 선택
        }
        """
        body = request.get_json(silent=True) or {}
        text = (body.get("text") or "").strip()
        lines = int(body.get("lines") or 3)
        if not text:
            return jsonify({"error": "text is required"}), 400

        # TODO: 여기에 실제 요약 모델/외부 API 호출 연결
        # 현재는 간단한 더미 요약(문장 분리 후 상위 N개)
        sents = _split_sentences(text)
        if not sents:
            return jsonify({"error": "no sentences parsed"}), 400

        n = max(1, lines)
        result = sents[:n]
        return jsonify({
            "lines": result,
            "count": len(result),
            "type": body.get("type") or "DEFAULT",
        }), 200

    # 필요하면 /summary/prompts, /batch 등 추가 엔드포인트도 여기에 정의
    return app


if __name__ == "__main__":
    # 콘솔 출력: 실행 컨텍스트 요약
    print(f"[flaskapi] ENV={APP_ENV} PORT={PORT}")
    print(f"[flaskapi] DATABASE_URL={DATABASE_URL}")
    print(f"[flaskapi] docker={running_in_docker()} base={BASE_DIR}")

    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, default=PORT)
    args = parser.parse_args()

    app = create_app()
    # host=0.0.0.0 → 같은 네트워크의 다른 기기/컨테이너에서도 접근 가능
    app.run(host="0.0.0.0", port=args.port, debug=(APP_ENV == "local"))
