package com.newnormallist.newsservice.news.service;

import com.newnormallist.newsservice.summarizer.client.SummarizerClient;
import com.newnormallist.newsservice.news.dto.SummaryRequest;
import com.newnormallist.newsservice.news.dto.SummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * NewsSummaryService
 * - 비즈니스 레이어(필요 시 캐시/DB 저장/권한/리트라이를 여기서)
 * - 현재는 SummarizerClient로 패스스루 호출만 수행
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NewsSummaryService {

    private final SummarizerClient summarizerClient;

    /** 컨트롤러에서 받은 요청을 그대로 요약 호출 */
    public SummaryResponse summarize(SummaryRequest req) {
        // (선택) 안전장치: 다른 호출 경로에서 들어올 수 있으니 가벼운 XOR 재검증
        boolean hasId = req.getNewsId() != null;
        boolean hasText = req.getText() != null && !req.getText().isBlank();
        if (hasId == hasText) {
            throw new IllegalArgumentException("newsId 또는 text 중 하나만 제공해야 합니다.");
        }

        log.debug("[NewsSummaryService] summarize start newsId={} type={} lines={}",
                req.getNewsId(), req.getType(), req.getLines());

        SummaryResponse res = summarizerClient.summarize(req);

        // (선택) TODO: 캐시/DB 저장 로직
        // if (hasId) { summaryRepository.upsert(...); }

        log.debug("[NewsSummaryService] summarize done newsId={} cached={} len={}",
                res.getNewsId(), res.isCached(),
                (res.getSummary() != null ? res.getSummary().length() : 0));

        return res;
    }

    /** 편의: newsId만으로 요약 (type/lines/promptOverride 생략 가능) */
    public SummaryResponse summarizeByNewsId(long newsId) {
        return summarizerClient.summarizeByNewsId(newsId, null, null, null);
    }

    /** 편의: newsId + 옵션으로 요약 */
    public SummaryResponse summarizeByNewsId(long newsId, String type, Integer lines, String promptOverride) {
        return summarizerClient.summarizeByNewsId(newsId, type, lines, promptOverride);
    }

    /** 편의: text만으로 요약 */
    public SummaryResponse summarizeByText(String text) {
        return summarizerClient.summarizeByText(text, null, null, null);
    }

    /** 편의: text + 옵션으로 요약 */
    public SummaryResponse summarizeByText(String text, String type, Integer lines, String promptOverride) {
        return summarizerClient.summarizeByText(text, type, lines, promptOverride);
    }
}
