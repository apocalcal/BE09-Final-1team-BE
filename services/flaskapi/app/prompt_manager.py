# app/prompt_manager.py
from typing import List, Dict, Optional, Tuple

class PromptManager:
    """
    - 뉴스 카테고리 문자열을 프롬프트 타입으로 그대로 사용.
    - 등록되지 않은 타입(카테고리)이면 DEFAULT로 치환.
    - get() : 프롬프트 텍스트만 반환
    - get_effective(): (실제 사용된 타입, 프롬프트 텍스트) 튜플 반환 → DB 저장 시 resolved_type을 그대로 쓰면 됨.
    """

    # ── Summarization Config ─────────────────────────────────────────────────────────
    LINES = 3  # 기본 요약 줄 수 (※ 줄 수 표기는 default에만 적용)

    # ── Generation Settings ─────────────────────────────────────────────────────────
    TEMPERATURE = 0.2
    # temperature=0.2 의미(샘플링 온도)
    # - 낮을수록 분포가 뾰족 → 결정적/일관적 출력(요약·사실형 작업에 적합)
    # - 높을수록 분포가 평평 → 다양/창의(브레인스토밍·창작에 적합)
    # - 수식: p = softmax(logits / T). T를 줄이면 높은 확률 토큰이 더 자주 선택됩니다.
    # - T≈0이면 greedy에 가깝고, 샘플링을 안 쓰면(beam/greedy) temperature 영향이 거의 없습니다.
    # - top_p/top_k와의 차이: temperature는 남겨둔 후보 ‘사이’의 확률 기울기, top_p/top_k는 후보의 범위.
    # - 권장 범위: 요약/코드 0.1~0.3, 창작 0.7~1.0. 본 세팅(0.2)은 사실 위주 요약에 최적화.

    GEN_PARAMS = {
        "temperature": TEMPERATURE,  # ↓ 낮을수록 확률 분포가 뾰족 → 일관적 결과(요약에 적합)

        "top_p": 1.0,                # nucleus sampling 상한(0~1). 1.0이면 사실상 '끄기'(전체 분포 사용).
        # 0.8~0.95로 낮추면 상위 토큰만 샘플링 → 산만함 감소, 보수적 출력.
        # temperature와 top_p를 동시에 크게 조정하면 예측 불가성 ↑ → 보통 둘 중 하나만 조정.

        # "max_tokens": 400,         # 생성(출력) 가능한 최대 토큰 수 상한.
        # 요약 길이 관리용. 너무 낮으면 문장 도중 절단될 수 있음.
        # (대략 3줄 요약: 120~200 토큰 안팎으로 시작해 보며 조정)

        # "presence_penalty": 0.0,   # 이미 등장한 '어떤' 토큰이라도 다시 쓰는 경향을 전반적으로 억제(+이면 억제).
        # 새 주제/어휘로 확장 유도. 범위 보통 [-2.0, 2.0].
        # 사실 요약은 0.0~0.2 권장(높이면 원문 용어 유지가 흔들릴 수 있음).

        # "frequency_penalty": 0.0,  # 같은 토큰을 '반복해서' 쓸수록 더 강하게 패널티(+이면 억제).
        # 중복 문구/어휘 반복을 줄이는 데 유용. 보통 0.0~0.3로 미세 조정.
        # 너무 높으면 필요한 핵심 용어도 과하게 바뀔 수 있음.
    }

    # 1) 프롬프트 세트
    PROMPTS: Dict[str, Dict[str, str]] = {
        "DEFAULT": {
            "default": f"기사의 핵심만 {LINES}줄로 간결하게 요약해줘.",
            "balanced": "중립적 톤으로 사실 위주로 요약. 날짜·수치·고유명사 정확히 유지.",
            "timeline": "시간 순서대로 사건 전개를 정렬해 타임라인으로 요약해줘.",
            "claims_vs_evidence": "원문 ‘주장’과 ‘근거’를 구분해 요약해줘. 추측·의견은 제외.",
            "impact": "핵심 쟁점과 파급효과(정책·시장·사회)를 중심으로 정리해줘.",
            "background_then_new": "배경은 간단히만 언급하고, 새로 보고된 사실 중심으로 구성해줘.",
            "stakeholders": "이해관계자(정부·기업·개인 등)별 핵심 메시지를 구분해 요약해줘.",
            "active_voice": "중복·수식어 최소화, 능동태 사용으로 핵심 사실만 요약해줘.",
            "limits_next_steps": "불확실성·한계·다음 단계(향후 일정/조치)를 포함해 정리해줘."
        },
        # 필요 시 카테고리별 세트를 추가 (예: POLITICS, ECONOMY, IT_SCIENCE 등)
        "NEWSLETTER": {
            "default": "카카오톡 뉴스레터용으로 핵심만 1줄로 요약해줘",
            "catchy": "뉴스레터 1줄 요약: 핵심 메시지를 임팩트 있게.",
        },
        "_GLOBAL": {
            "title": "이 기사 제목으로 쓸 수 있는 한 줄 헤드라인을 작성해줘",
            "sentiment_neutralize": "정치/이념적 편향 없이 중립적 톤으로 요약해줘 (1~2줄).",
        },
    }

    # 기본 타입
    DEFAULT_TYPE = "DEFAULT"

    # 2) 별칭/정규화 테이블 (카테고리 → 타입)
    #   - 뉴스 카테고리의 다양한 표기를 보정
    #   - 레거시('AIBOT')가 오면 DEFAULT로 흡수
    TYPE_ALIASES: Dict[str, str] = {
        "": "DEFAULT",
        "AIBOT": "DEFAULT",

        # 카테고리 표기 보정(선택)
        "WORLD": "INTERNATIONAL",
        "GLOBAL": "INTERNATIONAL",
        "IT": "IT_SCIENCE",
        "SCIENCE": "IT_SCIENCE",
        "AUTO": "VEHICLE",
        "CAR": "VEHICLE",
        "TRAVEL": "TRAVEL_FOOD",
        "FOOD": "TRAVEL_FOOD",
        "CULTURE": "ART",
    }

    # ──────────────────────────────────────────────────────────────────────────
    # 내부 유틸
    # ──────────────────────────────────────────────────────────────────────────
    @staticmethod
    def _normalize_key(s: Optional[str]) -> str:
        u = (s or "").upper().strip()
        return u.replace("-", "_").replace(" ", "_").replace("/", "_")

    @classmethod
    def _canon_type(cls, t: Optional[str]) -> str:
        """
        미지정/미등록 타입을 DEFAULT로 정규화.
        """
        u = cls._normalize_key(t)
        u = cls.TYPE_ALIASES.get(u, u or cls.DEFAULT_TYPE)
        return u if u in cls.PROMPTS else cls.DEFAULT_TYPE

    @staticmethod
    def _fmt(s: str, base: Dict) -> str:
        try:
            return s.format(**base)
        except KeyError:
            # 알 수 없는 placeholder는 그대로 둠
            return s

    # ──────────────────────────────────────────────────────────────────────────
    # 공개 API
    # ──────────────────────────────────────────────────────────────────────────
    @classmethod
    def get_many_by_types(
            cls,
            types: List[str],
            include_default: bool = True,
            **params
    ) -> List[Dict[str, str]]:
        """
        여러 타입을 받아 각 타입에 등록된 모든 프롬프트를 반환.
        타입이 미지정/미등록이면 DEFAULT로 매핑.
        반환: [{"id": "TYPE:key", "prompt": "..."}, ...]
        """
        results: List[Dict[str, str]] = []
        seen = set()
        base = {"lines": 3, **params}

        for t in (types or [cls.DEFAULT_TYPE]):
            g = cls._canon_type(t)
            entries = cls.PROMPTS.get(g, {})
            for k, v in entries.items():
                if k == "default" and not include_default:
                    continue
                pid = f"{g}:{k}"
                if pid in seen:
                    continue
                seen.add(pid)
                results.append({"id": pid, "prompt": cls._fmt(v, base)})
        return results

    @classmethod
    def get(cls, prompt_or_id: Optional[str], type_: Optional[str], **params) -> str:
        """
        프롬프트 텍스트만 반환.
        - type_이 미지정/미등록이면 DEFAULT로 처리
        - prompt_or_id가 없으면 해당 타입의 default 사용
        - "TYPE:key" 표기 지원, 키가 없으면 DEFAULT로 폴백
        - 커스텀 문자열이면 그대로 사용
        """
        base = {"lines": 3, **params}
        t = cls._canon_type(type_)
        s = (prompt_or_id or "").strip()

        if not s:
            return cls._fmt(cls.PROMPTS[t].get("default", cls.PROMPTS[cls.DEFAULT_TYPE]["default"]), base)

        # "TYPE:key"
        if ":" in s:
            tt, key = s.split(":", 1)
            tt = cls._canon_type(tt)
            val = cls.PROMPTS.get(tt, {}).get(key)
            if val:
                return cls._fmt(val, base)
            # DEFAULT에서 같은 key 재시도
            val = cls.PROMPTS[cls.DEFAULT_TYPE].get(key)
            if val:
                return cls._fmt(val, base)
            # 그래도 없으면 DEFAULT.default
            return cls._fmt(cls.PROMPTS[cls.DEFAULT_TYPE]["default"], base)

        # 현재 타입에서 키 조회
        val = cls.PROMPTS.get(t, {}).get(s)
        if val:
            return cls._fmt(val, base)

        # DEFAULT에서 키 조회
        val = cls.PROMPTS[cls.DEFAULT_TYPE].get(s)
        if val:
            return cls._fmt(val, base)

        # 커스텀 문자열
        return cls._fmt(s, base)

    @classmethod
    def get_effective(
            cls,
            prompt_or_id: Optional[str],
            type_candidate: Optional[str],
            **params
    ) -> Tuple[str, str]:
        """
        (resolved_type, prompt_text) 반환.
        - type_candidate가 PROMPTS에 없으면 DEFAULT로 치환
        - prompt_or_id가 없으면 resolved_type의 default 사용
        - "TYPE:key" 지원 및 DEFAULT 폴백
        """
        resolved_type = cls._canon_type(type_candidate)
        text = cls.get(prompt_or_id, resolved_type, **params)
        # 주의: get() 내부에서도 resolved_type이 바뀌진 않게 설계 (TYPE:key의 TYPE은 별개)
        return resolved_type, text
