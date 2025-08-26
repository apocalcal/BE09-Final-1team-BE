package com.newnormallist.newsservice.summarizer.controller;

import com.newnormallist.newsservice.news.dto.*;
import com.newnormallist.newsservice.summarizer.dto.AdhocSummaryRequest;
import com.newnormallist.newsservice.summarizer.dto.SummaryRequest;
import com.newnormallist.newsservice.summarizer.dto.SummaryResponse;
import com.newnormallist.newsservice.summarizer.service.NewsSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "News Summary", description = "뉴스 요약 API")
public class NewsSummaryController {

    private final NewsSummaryService service;

    @Operation(
            summary = "요약 (바디 기반: newsId 또는 text)",
            description = """
            - body.newsId가 있으면: ID 기반 요약 (DB 캐시 우선, force 지원)
            - body.text가 있으면: 텍스트 임시 요약 (DB 저장 안 함)
            - 둘 다 있으면: newsId 우선
            """
    )
    @PostMapping(
            value = "/summary",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public SummaryResponse summarize(@Valid @RequestBody SummaryRequest body){
        log.debug("[POST] /api/news/summary body={}", body);

        if (body.getNewsId() != null) {
            // 서비스용 DTO로 매핑 (기존 SummaryRequest 그대로 재사용)
            SummaryRequest req = SummaryRequest.builder()
                    .type(body.getType())
                    .lines(body.getLines())
                    .promptOverride(body.getPrompt())
                    .force(body.getForce())
                    .build();
            return service.getOrCreateSummary(body.getNewsId(), req);
        }

        if (StringUtils.hasText(body.getText())) {
            // 텍스트 임시 요약 (기존 AdhocSummaryRequest 재사용)
            AdhocSummaryRequest req = AdhocSummaryRequest.builder()
                    .text(body.getText())
                    .type(body.getType())
                    .lines(body.getLines())
                    .promptOverride(body.getPrompt())
                    .build();
            return service.summarizeText(req);
        }

        throw new IllegalArgumentException("newsId 또는 text 중 하나는 필수입니다.");
    }
}
