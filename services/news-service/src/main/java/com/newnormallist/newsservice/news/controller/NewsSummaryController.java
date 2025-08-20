package com.newsservice.news.controller;

import com.newsservice.news.dto.SummaryOptions;
import com.newsservice.news.dto.SummaryRequest;
import com.newsservice.news.dto.SummaryResponse;
import com.newsservice.news.service.NewsSummaryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 경로 정책:
 * - 게이트웨이에서 RewritePath 미사용 시 그대로 /api/news/** 로 전달
 * - 본 컨트롤러의 basePath("/api/news")와 정확히 일치해야 404 방지
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Tag(
        name = "News Summary",
        description = "크롤링 DB를 우선 사용하고, 없으면 요약 생성 후 DB에 저장하는 뉴스 요약 API"
)
public class NewsSummaryController {

    private final NewsSummaryService service;

    /** 상세페이지 요약 버튼(캐시 우선, 미존재 시 생성) */
    @PostMapping("/{newsId}/summary")
    public SummaryResponse summarize(
            @PathVariable @Positive long newsId,
            @RequestBody(required = false) @Valid SummaryOptions opts
    ) {
        log.info("POST /api/news/{}/summary opts={}", newsId, opts);
        return service.summarizeFromDb(newsId, opts);
    }

    /** 캐시 조회 전용 (없으면 summary=null 로 반환) */
    @GetMapping("/{newsId}/summary")
    public SummaryResponse getCached(
            @PathVariable @Positive long newsId,
            @RequestParam(defaultValue = "DEFAULT") String type,
            @RequestParam(defaultValue = "3") Integer lines,
            @RequestParam(required = false) String prompt
    ) {
        log.info("GET /api/news/{}/summary type={} lines={} prompt={}", newsId, type, lines, prompt);
        return service.getCached(newsId, type, lines, prompt);
    }

    /** (선택) ad-hoc 텍스트 요약 */
    @PostMapping("/summary")
    public SummaryResponse summarizeText(@Valid @RequestBody SummaryRequest req) {
        log.info("POST /api/news/summary (raw text) type={} lines={} prompt={}", req.type(), req.lines(), req.prompt());
        return service.summarizeRawText(req);
    }
}