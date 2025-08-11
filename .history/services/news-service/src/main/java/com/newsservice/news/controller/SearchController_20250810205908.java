package com.newsservice.news.controller;

import com.newsservice.news.dto.NewsListResponse;
import com.newsservice.news.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {
    
    @Autowired
    private NewsService newsService;

    /**
     * 키워드 검색 (기본)
     */
    @GetMapping
    public ResponseEntity<Page<NewsListResponse>> searchNews(
            @RequestParam String query,
            @RequestParam(required = false) String sortBy, // "latest", "popular", "relevance"
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String press,
            Pageable pageable) {
        Page<NewsListResponse> news = newsService.searchNews(query, sortBy, category, press, pageable);
        return ResponseEntity.ok(news);
    }

    /**
     * 자동완성 API - 실시간 검색어 제안
     */
    @GetMapping("/autocomplete")
    public ResponseEntity<List<String>> getAutocompleteSuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        List<String> suggestions = newsService.getAutocompleteSuggestions(query, limit);
        return ResponseEntity.ok(suggestions);
    }

    /**
     * 인기 검색어 조회
     */
    @GetMapping("/trending-keywords")
    public ResponseEntity<List<String>> getTrendingKeywords(
            @RequestParam(defaultValue = "10") int limit) {
        List<String> trendingKeywords = newsService.getTrendingKeywords(limit);
        return ResponseEntity.ok(trendingKeywords);
    }

    /**
     * 검색 통계 - 언론사별, 카테고리별 통계
     */
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getSearchStats(
            @RequestParam String query) {
        Map<String, Object> stats = newsService.getSearchStats(query);
        return ResponseEntity.ok(stats);
    }

    /**
     * 관련 키워드 추천
     */
    @GetMapping("/related-keywords")
    public ResponseEntity<List<String>> getRelatedKeywords(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {
        List<String> relatedKeywords = newsService.getRelatedKeywords(query, limit);
        return ResponseEntity.ok(relatedKeywords);
    }

    /**
     * 검색 결과 하이라이팅 포함
     */
    @GetMapping("/highlight")
    public ResponseEntity<Page<NewsListResponse>> searchNewsWithHighlight(
            @RequestParam String query,
            Pageable pageable) {
        Page<NewsListResponse> news = newsService.searchNewsWithHighlight(query, pageable);
        return ResponseEntity.ok(news);
    }

    /**
     * 고급 검색 - 여러 조건 조합
     */
    @GetMapping("/advanced")
    public ResponseEntity<Page<NewsListResponse>> advancedSearch(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String press,
            @RequestParam(required = false) String reporter,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @RequestParam(required = false) Boolean trusted,
            @RequestParam(defaultValue = "latest") String sortBy,
            Pageable pageable) {
        Page<NewsListResponse> news = newsService.advancedSearch(
                query, category, press, reporter, startDate, endDate, trusted, sortBy, pageable);
        return ResponseEntity.ok(news);
    }

    /**
     * 언론사별 뉴스 조회
     */
    @GetMapping("/press/{press}")
    public ResponseEntity<Page<NewsListResponse>> getNewsByPress(
            @PathVariable String press,
            Pageable pageable) {
        Page<NewsListResponse> news = newsService.getNewsByPress(press, pageable);
        return ResponseEntity.ok(news);
    }

    /**
     * 기간별 뉴스 조회
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<NewsListResponse>> getNewsByDateRange(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        // 기본값 설정: startDate가 없으면 7일 전, endDate가 없으면 현재 시간
        LocalDateTime defaultStartDate = startDate != null ? startDate : LocalDateTime.now().minusDays(7);
        LocalDateTime defaultEndDate = endDate != null ? endDate : LocalDateTime.now();
        
        List<NewsListResponse> newsList = newsService.getNewsByDateRange(defaultStartDate, defaultEndDate);
        return ResponseEntity.ok(newsList);
    }
}
