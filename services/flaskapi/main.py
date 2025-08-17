# main.py
from app import create_app
import os
import socket

app = create_app()

print("DATABASE_URL =", os.getenv("DATABASE_URL"))

# --- 실행 환경 감지 (도커 여부) ---
def running_in_docker() -> bool:
    return os.path.exists("/.dockerenv") or os.getenv("RUN_IN_DOCKER") == "1"

# --- Eureka 설정 값 결정 ---
if running_in_docker():
    # 컨테이너 내부: 도커 네트워크 호스트명 사용
    EUREKA_SERVER = os.getenv("EUREKA_SERVER", "http://discovery-service:8761/eureka/")
    INSTANCE_HOST = os.getenv("INSTANCE_HOST", "flaskapi")
else:
    # 로컬 venv: localhost 사용
    EUREKA_SERVER = os.getenv("EUREKA_SERVER", "http://localhost:8761/eureka/")
    # Windows에서 gethostname()이 애매할 수 있으므로 localhost가 가장 안전
    INSTANCE_HOST = os.getenv("INSTANCE_HOST", "localhost")

APP_NAME = os.getenv("APP_NAME", "FLASKAPI")
PORT     = int(os.getenv("PORT", "5000"))

print("[Eureka] eureka_server =", EUREKA_SERVER)
print("[Eureka] app_name     =", APP_NAME)
print("[Eureka] instance_host =", INSTANCE_HOST)
print("[Eureka] instance_port =", PORT)

# --- py-eureka-client 등록 (버전차 최소 인자) ---
try:
    import py_eureka_client.eureka_client as eureka_client
    eureka_client.init(
        eureka_server=EUREKA_SERVER,
        app_name=APP_NAME,
        instance_host=INSTANCE_HOST,
        instance_port=PORT,
    )
    print("[Eureka] 등록 성공")
except Exception as e:
    import traceback; traceback.print_exc()
    print("[Eureka] 등록 실패:", e)

@app.get("/")
def ping():
    return {"ok": True, "msg": "alive"}

if __name__ == "__main__":
    # 로컬/도커 공통: 외부에서 접근 가능하도록 0.0.0.0
    app.run(host="0.0.0.0", port=PORT)
