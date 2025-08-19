# app/__init__.py
from flask import Flask, request, jsonify, current_app
import os
from pathlib import Path
from sqlalchemy.exc import OperationalError
# 핵심: 오직 extensions.db만 초기화에 사용
from .extensions import db  # ← 여기! (원래 .models import db 였다면 교체)


def _normalize_sqlite_uri(app, uri: str) -> str:
    """SQLite URI를 절대경로/정상형식으로 정규화."""
    if not uri.startswith("sqlite"):
        return uri  # MySQL 등은 그대로

    # sqlite:///something.db  → 상대경로라면 instance_path 기준으로 절대화
    if uri.startswith("sqlite:///") and not uri.startswith("sqlite:////"):
        rel = uri.replace("sqlite:///", "", 1)
        abs_path = (Path(app.instance_path) / rel).resolve()
        return f"sqlite:///{abs_path.as_posix()}"

    # sqlite:////C:/...  형식은 유지. 다만 백슬래시는 금지, as_posix로 교체 필요 시만 처리
    if uri.startswith("sqlite:////"):
        # 추출해서 posix로 보정
        raw = uri.replace("sqlite:////", "", 1)
        p = Path("/" + raw)  # 앞의 슬래시 하나는 드라이브 레터 전용
        return f"sqlite:////{p.as_posix().lstrip('/')}"

    # 그 외 변형이 올 경우(매우 드묾): instance/summary.db 로 폴백
    db_file = Path(app.instance_path) / "summary.db"
    return f"sqlite:///{db_file.as_posix()}"

def create_app():
    app = Flask(__name__, instance_relative_config=True)
    Path(app.instance_path).mkdir(parents=True, exist_ok=True)

    # 1) 환경설정 로드
    env = os.getenv("FLASK_ENV", "").lower()
    if env == "production":
        from .config import ProdConfig as ActiveConfig
    else:
        from .config import DevConfig as ActiveConfig
    app.config.from_object(ActiveConfig)

    # (개발 편의) SQL 로그 보고 싶으면 Dev에서만 켜기
    if env != "production":
        app.config.setdefault("SQLALCHEMY_ECHO", True)

    # 2) DB URI 확정
    db_uri = os.getenv("DATABASE_URL") or app.config.get("SQLALCHEMY_DATABASE_URI")
    if not db_uri:
        db_file = Path(app.instance_path) / "summary.db"
        db_uri = f"sqlite:///{db_file.as_posix()}"

    # 3) SQLite URI 정규화 + 엔진 옵션
    db_uri = _normalize_sqlite_uri(app, db_uri)
    app.config["SQLALCHEMY_DATABASE_URI"] = db_uri
    app.config.setdefault("SQLALCHEMY_TRACK_MODIFICATIONS", False)

    engine_opts = app.config.get("SQLALCHEMY_ENGINE_OPTIONS", {})
    if db_uri.startswith("sqlite"):
        connect_args = engine_opts.get("connect_args", {})
        connect_args.setdefault("check_same_thread", False)
        engine_opts["connect_args"] = connect_args
    engine_opts.setdefault("pool_pre_ping", True)
    app.config["SQLALCHEMY_ENGINE_OPTIONS"] = engine_opts

    # 4) DB init & 테이블 생성
    db.init_app(app)  # extensions.db 를 초기화
    with app.app_context():
        try:
            # 모델을 '정의만' 임포트해서 메타데이터를 로딩
            from . import models  # (models 내부에서는 반드시 from .extensions import db 사용)
            db.create_all()
        except OperationalError as e:
            print("[DB][OperationalError]", e)
            print("[DB][Hint] 상위 폴더/권한/URI 형식을 다시 확인하세요.")
            raise

    # 5) 블루프린트 등록
    from .routes.summary_route import summary_bp
    app.register_blueprint(summary_bp)

    # 6) 헬스/디버그 라우트
    @app.get("/")
    def health():
        return {"ok": True}

    @app.get("/__routes")
    def __routes():
        return {
            "cwd": os.getcwd(),
            "instance_path": app.instance_path,
            "db_uri": app.config.get("SQLALCHEMY_DATABASE_URI"),
            "routes": [{"rule": r.rule, "methods": sorted(list(r.methods))} for r in app.url_map.iter_rules()],
        }

    # 추가: 실제 SQLite 파일이 무엇인지 확인 (혼선 방지)
    @app.get("/__db")
    def __db():
        try:
            uri = str(db.engine.url)
            info = {"engine_url": uri}
            if uri.startswith("sqlite"):
                rows = db.session.execute(db.text("PRAGMA database_list")).mappings().all()
                info["sqlite_database_list"] = [dict(r) for r in rows]
            return info
        except Exception as e:
            current_app.logger.exception("__db failed")
            return {"error": str(e)}, 500

    @app.before_request
    def _log_req():
        print(">>", request.method, request.path, request.headers.get("Content-Type"))

    @app.errorhandler(404)
    def _not_found(e):
        if request.path.startswith("/summary"):
            return jsonify({
                "error": "Not Found",
                "path": request.path,
                "hint": "아래 routes에서 /summary 엔드포인트가 있는지 확인하세요.",
                "routes": [r.rule for r in app.url_map.iter_rules()]
            }), 404
        return e, 404

    # 7) 부팅 로그
    print("[BOOT] CWD:", os.getcwd())
    print("[BOOT] instance_path:", app.instance_path)
    print("[BOOT] SQLALCHEMY_DATABASE_URI:", app.config["SQLALCHEMY_DATABASE_URI"])

    return app