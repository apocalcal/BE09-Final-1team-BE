# main.py
from app import create_app
import os
import socket

app = create_app()

print("DATABASE_URL =", os.getenv("DATABASE_URL"))

# --- 기본 헬스/핑 ---
@app.get("/")
def root():
    return {"ok": True, "msg": "alive"}, 200

@app.get("/health")
def health():
    # 필요하면 여기서 필수 ENV, DB 접근 등 점검 후 500 반환 로직 추가
    return {"status": "ok"}, 200


# --- 실행 환경 감지 (도커 여부) ---
def running_in_docker() -> bool:
    return os.path.exists("/.dockerenv") or os.getenv("RUN_IN_DOCKER") == "1"

def is_true(v) -> bool:
    return str(v).lower() in ("1", "true", "yes", "on")

# --- 설정 ---
PORT = int(os.getenv("PORT", "5000"))

# Eureka 토글 (기본: 비활성)
EUREKA_ENABLED = is_true(os.getenv("EUREKA_ENABLED", "false"))
APP_NAME = os.getenv("APP_NAME", "FLASKAPI")

if running_in_docker():
    # 컨테이너 내부: 도커 네트워크 호스트명 사용
    DEFAULT_EUREKA = "http://discovery-service:8761/eureka/"
    DEFAULT_HOST = "flaskapi"
else:
    # 로컬 venv: localhost 사용
    DEFAULT_EUREKA = "http://localhost:8761/eureka/"
    DEFAULT_HOST = "localhost"

# EUREKA_SERVER = os.getenv("EUREKA_SERVER", DEFAULT_EUREKA)
# INSTANCE_HOST = os.getenv("INSTANCE_HOST", DEFAULT_HOST)

# # --- Eureka 등록 (옵션⭐) ---
# if EUREKA_ENABLED and EUREKA_SERVER:
#     try:
#         import py_eureka_client.eureka_client as eureka_client
#         # 필요 시 should_register/fetch, heartbeat 등 옵션 추가 가능
#         eureka_client.init(
#             eureka_server=EUREKA_SERVER,
#             app_name=APP_NAME,
#             instance_host=INSTANCE_HOST,
#             instance_port=PORT,
#             should_register=True,
#             should_fetch=False,
#         )
#         print(f"[Eureka] 등록 완료 server={EUREKA_SERVER} app={APP_NAME} host={INSTANCE_HOST}:{PORT}")
#     except Exception as e:
#         # 등록 실패해도 API 자체는 동작하도록 조용히 처리
#         print("[Eureka] 등록 실패:", e)
# else:
#     print("[Eureka] 비활성화됨(EUREKA_ENABLED=false) 또는 서버 미지정")

if __name__ == "__main__":
    # 로컬 실행용(개발 서버). 운영은 gunicorn 사용.
    app.run(host="0.0.0.0", port=PORT)
