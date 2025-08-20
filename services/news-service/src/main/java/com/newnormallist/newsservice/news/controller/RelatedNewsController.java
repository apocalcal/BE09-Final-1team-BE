package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.RelatedNewsResponseDto;
import com.newnormallist.newsservice.news.service.RelatedNewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
public class RelatedNewsController {

    private final RelatedNewsService relatedNewsService;

    /**
     * 뉴스 ID로 연관뉴스를 조회합니다.
     * @param newsId 조회할 뉴스 ID
     * @return 연관뉴스 목록 (최대 4개)
     */
    @GetMapping("/{newsId}/related")
    public ResponseEntity<List<RelatedNewsResponseDto>> getRelatedNews(@PathVariable Long newsId) {
        log.info("연관뉴스 조회 요청: newsId = {}", newsId);
        
        List<RelatedNewsResponseDto> relatedNews = relatedNewsService.getRelatedNews(newsId);
        
        log.info("연관뉴스 조회 완료: newsId = {}, 연관뉴스 개수 = {}", newsId, relatedNews.size());
        
        return ResponseEntity.ok(relatedNews);
    }
}
