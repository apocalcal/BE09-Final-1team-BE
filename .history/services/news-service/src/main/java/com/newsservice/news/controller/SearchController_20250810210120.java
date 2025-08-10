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

@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
public class SearchController {
    
    @Autowired
    private NewsService newsService;

    /**
     * 키워드 검색
     */
    @GetMapping
    public ResponseEntity<Page<NewsListResponse>> searchNews(
            @RequestParam String query,
            Pageable pageable) {
        Page<NewsListResponse> news = newsService.searchNews(query, pageable);
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
