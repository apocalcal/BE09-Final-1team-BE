# routes/summary_route.py
from flask import Blueprint, request, jsonify, current_app
from sqlalchemy.exc import SQLAlchemyError
from ..extensions import db
from ..models import News, NewsSummary
from ..prompt_manager import PromptManager
from ..services.summarizer import summarize

summary_bp = Blueprint("summary", __name__, url_prefix="/summary")
MAX_PROMPTS = 10  # 안전상 상한

def _parse_bool(data, key, default=False):
    val = data.get(key, default)
    if isinstance(val, bool):
        return val
    return str(val).strip().lower() in ("1","true","yes","on")

def _parse_lines(data, default=3, min_=1, max_=10) -> int:
    try:
        v = int(data.get("lines", default))
        return max(min_, min(v, max_))
    except Exception:
        return default

def _aggregate_summaries(items, lines: int) -> str:
    """
    items: [{"id": "TYPE:key", "summary": "..."}]
    """
    merged = "\n\n".join(f"[{it['id']}]\n{it['summary']}" for it in items if it.get("summary"))
    meta_prompt = (
        "다음 여러 요약본을 통합하라.\n"
        "- 중복/반복 제거\n"
        "- 서로 상충되면 보수적으로 기술(확정어 피함)\n"
        "- 숫자/고유명사 보존\n"
        f"- 최종 출력은 {lines}줄, 각 줄은 한 문장\n"
    )
    return summarize(merged, meta_prompt)

def _resolve_prompt_and_type(data, news) -> tuple[str, str, int]:
    """
    반환: (resolved_type, prompt_text, lines)
    - type 후보: data.type/summary_type → 없으면 news.category
    - PromptManager가 미등록 타입은 DEFAULT로 정규화
    """
    type_candidate = data.get("summary_type") or data.get("type") or (getattr(news, "category", None) if news else None)
    lines = _parse_lines(data, default=3)
    resolved_type, prompt_text = PromptManager.get_effective(
        prompt_or_id=data.get("prompt"),
        type_candidate=type_candidate,
        lines=lines
    )
    return resolved_type, prompt_text, lines

@summary_bp.route("/", methods=["POST"])
@summary_bp.route("", methods=["POST"])
def create_summary():
    data = request.get_json(force=True)

    news_id = data.get("news_id")
    text = data.get("text")

    news = None
    if not text:
        if not news_id:
            return jsonify({"error": "text 또는 news_id가 필요합니다."}), 400
        news = News.query.get(news_id)
        if not news:
            return jsonify({"error": "뉴스가 없습니다."}), 404
        text = getattr(news, "content", None) or getattr(news, "body", None)
        if not text or not str(text).strip():
            return jsonify({"error": "뉴스 본문이 비어 있습니다."}), 400

    # 모드 선택: 단일(single, 기본) / 종합(ensemble)
    ensemble = _parse_bool(data, "ensemble", default=True)

    # 타입/프롬프트 결정 (미등록 타입이면 DEFAULT로 정규화)
    resolved_type, prompt_text, lines = _resolve_prompt_and_type(data, news)

    # 캐시: 단일 모드에만 적용 (ensemble은 결과가 매번 달라질 수 있으므로 캐시 생략 or 덮어쓰기)
    if not ensemble and news_id:
        cached = NewsSummary.query.filter_by(news_id=news_id, summary_type=resolved_type).first()
        if cached:
            return jsonify({
                "summary": cached.summary_text,
                "cached": True,
                "resolved_type": resolved_type,
                "lines": lines,
                "ensemble": False
            }), 200

    # 요약 생성
    try:
        if ensemble:
            # 해당 타입의 모든 프롬프트 실행 → 통합
            items = PromptManager.get_many_by_types([resolved_type], include_default=True, lines=lines)[:MAX_PROMPTS]
            partials = []
            for it in items:
                try:
                    s = summarize(text, it["prompt"])
                except Exception as e:
                    s = f"[ERROR] summarize 실패: {e}"
                partials.append({"id": it["id"], "summary": s})
            summary_text = _aggregate_summaries(partials, lines)
        else:
            summary_text = summarize(text, prompt_text)
    except Exception as e:
        current_app.logger.exception("summarize failed (/summary)")
        return jsonify({"error": "summarize failed", "detail": str(e)}), 500

    # 저장 (이 엔드포인트는 기본 저장)
    row = NewsSummary.query.filter_by(news_id=news_id, summary_type=resolved_type).first()
    if row:
        # ensemble이면 덮어쓰길 원하는지 선택. 기본: 덮어쓰기.
        row.summary_text = summary_text
    else:
        db.session.add(NewsSummary(news_id=news_id, summary_type=resolved_type, summary_text=summary_text))
    try:
        db.session.commit()
    except SQLAlchemyError as e:
        current_app.logger.exception("DB commit failed (/summary)")
        db.session.rollback()
        return jsonify({"error": "DB commit failed", "detail": str(e)}), 500

    return jsonify({
        "summary": summary_text,
        "cached": False,
        "resolved_type": resolved_type,
        "lines": lines,
        "ensemble": ensemble
    }), 201
