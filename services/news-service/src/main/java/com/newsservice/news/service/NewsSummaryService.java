package com.newsservice.news.service;

import com.newsservice.news.dto.SummaryOptions;
import com.newsservice.news.dto.SummaryResponse;
import com.newsservice.news.repository.NewsRepository;
import com.newsservice.news.repository.NewsSummaryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

/**
 * 요약 서비스 – 캐시 우선 → 미스 시 Flask 요약기 호출 → 저장(upsert).
 * 캐시 키: (newsId, type)
 * lines 는 DB 저장 없이 "표시/정책" 용도만 사용.
 */

// Service
@Slf4j
@Validated
@Service
@RequiredArgsConstructor
public class NewsSummaryService {
    private final NewsRepository newsRepository;
    private final NewsSummaryRepository summaryRepository;
    private final SummarizerClient summarizer;

    private static final String DEFAULT_TYPE = "DEFAULT";
    private static final int DEFAULT_LINES = 3;
    private static final String DEFAULT_PROMPT = "default";

    /** 캐시 조회 전용 (없으면 summary=null, fromCache=false) */
    @Transactional(readOnly = true)
    public SummaryResponse getCached(long newsId, String type, Integer lines, String prompt) {
        final String t = normalizeType(type);
        final int    l = normalizeLines(lines);
        final String p = normalizePrompt(prompt);

        return summaryRepository.findByNewsIdAndResolvedTypeAndLinesAndPromptKey(newsId, t, l, p)
                .map(e -> toResponse(e, true))
                .orElseGet(() -> new SummaryResponse(newsId, t, l, p, null, false));
    }

    /* 여기서부터 시작 ----------- */

    /** DB 본문 우선 → 캐시 없으면 생성/저장 → 반환 */
    @Transactional
    public SummaryResponse summarizeFromDb(long newsId, SummaryOptions opts) {
        final String t = normalizeType(opts != null ? opts.type()  : null);
        final int    l = normalizeLines(opts != null ? opts.lines() : null);
        final String p = normalizePrompt(opts != null ? opts.prompt(): null);

        // 1) 캐시 조회
        Optional<NewsSummary> hit = summaryRepository
                .findByNewsIdAndResolvedTypeAndLinesAndPromptKey(newsId, t, l, p);
        if (hit.isPresent()) {
            log.debug("cache hit newsId={} type={} lines={} prompt={}", newsId, t, l, p);
            return toResponse(hit.get(), true);
        }

        // 2) 본문 로드
        News news = newsRepo.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("news not found: " + newsId));

        // 3) 요약 생성 (Flask)
        String summary = summarizer.summarizeByNewsId(newsId, t, l, p);
        if (!StringUtils.hasText(summary)) {
            throw new IllegalStateException("summarizer returned empty summary");
        }

        // 4) 저장 (UNIQUE(news_id,resolved_type,lines,prompt_key) 가정)
        NewsSummary entity = NewsSummary.builder()
                .newsId(newsId)
                .resolvedType(t)
                .lines(l)
                .promptKey(p)
                .summary(summary)
                .build();

        NewsSummary saved;
        try {
            saved = summaryRepository.save(entity);
        } catch (EntityExistsException | org.springframework.dao.DataIntegrityViolationException e) {
            // 동시성으로 인한 중복 INSERT → 이미 저장된 값 재조회
            log.warn("duplicate key on save; reloading existing summary. key=[{},{},{},{}]", newsId, t, l, p);
            saved = summaryRepository
                    .findByNewsIdAndResolvedTypeAndLinesAndPromptKey(newsId, t, l, p)
                    .orElseThrow(() -> e);
        }

        return toResponse(saved, false);
    }

    /** (선택) ad-hoc 텍스트 요약 */
    @Transactional(readOnly = true)
    public SummaryResponse summarizeRawText(SummaryRequest req) {
        final String t = normalizeType(req.type());
        final int    l = normalizeLines(req.lines());
        final String p = normalizePrompt(req.prompt());

        String summary = summarizer.summarizeText(req.text(), t, l, p);
        if (!StringUtils.hasText(summary)) {
            throw new IllegalStateException("summarizer returned empty summary");
        }
        // newsId 없음(-1로 반환)
        return new SummaryResponse(-1L, t, l, p, summary, false);
    }

    // ───────────────────────── helpers ─────────────────────────

    private static String normalizeType(String type) {
        String v = (type == null || type.isBlank()) ? DEFAULT_TYPE : type.trim();
        return v.toUpperCase();
    }

    private static int normalizeLines(Integer lines) {
        if (lines == null) return DEFAULT_LINES;
        return Math.max(1, Math.min(10, lines));
    }

    private static String normalizePrompt(String prompt) {
        String v = (prompt == null || prompt.isBlank()) ? DEFAULT_PROMPT : prompt.trim();
        return v;
    }

    private static SummaryResponse toResponse(NewsSummary e, boolean fromCache) {
        return new SummaryResponse(
                e.getNewsId(),
                e.getResolvedType(),
                e.getLines(),
                e.getPromptKey(),
                e.getSummary(),
                fromCache
        );
    }
}
