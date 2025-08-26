package com.newnormallist.newsservice.summarizer.service;

import com.newnormallist.newsservice.summarizer.dto.AdhocSummaryRequest;
import com.newnormallist.newsservice.summarizer.dto.SummaryRequest;
import com.newnormallist.newsservice.summarizer.dto.SummaryResponse;
import com.newnormallist.newsservice.news.entity.News;
import com.newnormallist.newsservice.summarizer.entity.NewsSummaryEntity;
import com.newnormallist.newsservice.news.repository.NewsRepository;
import com.newnormallist.newsservice.summarizer.repository.NewsSummaryRepository;
import com.newnormallist.newsservice.summarizer.client.SummarizerClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsSummaryService {

    private final NewsRepository newsRepository;
    private final NewsSummaryRepository summaryRepository;
    private final SummarizerClient summarizerClient;

    private static final int DEFAULT_LINES = 3;

    private String normalizeType(String t) {
        String u = (t == null ? "" : t.trim()).toUpperCase().replace('-', '_');
        if (u.isEmpty()) return "DEFAULT";
        if ("AIBOT".equals(u)) return "DEFAULT"; // ← 핵심
        // 레거시 AIBOT >> DEFAULT로 바꾸기
        // if (!ALLOWED_TYPES.contains(u)) return "DEFAULT";
        return u;
    }


    private int normalizeLines(Integer l) {
        return (l == null || l < 1 || l > 10) ? DEFAULT_LINES : l;
    }

    /** DB 캐시 단건 조회 (키: newsId + summaryType) */
    @Transactional(readOnly = true)
    public Optional<NewsSummaryEntity> findCached(long newsId, String summaryType) {
        return summaryRepository.findByNewsIdAndSummaryType(newsId, summaryType);
    }

    /** 1) 상세페이지: ID 기반 요약 (DB 캐시 우선, 없으면 생성 후 저장 / force=true면 재생성) */
    @Transactional
    public SummaryResponse getOrCreateSummary(long newsId, SummaryRequest req) {
        final String summaryType = normalizeType(req.getType());
        final int lines = normalizeLines(req.getLines());
        final boolean force = Boolean.TRUE.equals(req.getForce());

        // 1) 캐시 반환 (force가 아니면)
        if (!force) {
            var cached = findCached(newsId, summaryType);
            if (cached.isPresent()) {
                var e = cached.get();
                return SummaryResponse.builder()
                        .newsId(newsId)
                        .resolvedType(e.getSummaryType())
                        .lines(e.getLines())
                        .summary(e.getSummaryText())
                        .cached(true)
//                        .stale(false) // 필요시 사용
                        .build();
            }
        }

        // 2) 본문 로드
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new IllegalArgumentException("뉴스를 찾을 수 없습니다: id=" + newsId));
        String content = Optional.ofNullable(news.getContent()).orElse("");

        // 3) Flask 요약 호출 (SummarizerClient)
        String summary = summarizerClient.summarize(
                content,
                summaryType,
                lines,
                req.getPrompt() // DTO에 getPrompt()가 있어야 합니다(없으면 alias 추가)
        );

        // 4) upsert: (news_id, summary_type)로 존재하면 갱신, 없으면 생성
        NewsSummaryEntity saved = summaryRepository
                .findByNewsIdAndSummaryType(newsId, summaryType)
                .map(e -> {
                    e.setSummaryText(summary);
                    e.setLines(lines); // lines는 유니크 키에 포함되지 않아 최신 요청값으로 덮어씀
                    return summaryRepository.save(e);
                })
                .orElseGet(() -> summaryRepository.save(
                        NewsSummaryEntity.builder()
                                .newsId(newsId)
                                .summaryType(summaryType)
                                .lines(lines)
                                .summaryText(summary)
                                .build()
                ));

        // 5) 응답
        return SummaryResponse.builder()
                .newsId(newsId)
                .resolvedType(saved.getSummaryType())
                .lines(saved.getLines())
                .summary(saved.getSummaryText())
                .cached(false)
//                .stale(false)
                .build();
    }

    /** 2) 텍스트 임시 요약 (DB 저장 안 함) */
    @Transactional(readOnly = true)
    public SummaryResponse summarizeText(AdhocSummaryRequest req) {
        final String summaryType = normalizeType(req.getType());
        final int lines = normalizeLines(req.getLines());

        String text = Optional.ofNullable(req.getText()).orElse("");
        String summary = summarizerClient.summarize(
                text,
                summaryType,
                lines,
                req.getPrompt() // DTO에 getPrompt()가 있어야 합니다(없으면 alias 추가)
        );

        return SummaryResponse.builder()
                .newsId(0L) // id 없음
                .resolvedType(summaryType)
                .lines(lines)
                .summary(summary)
                .cached(false)
//                .stale(false)
                .build();
    }
}
