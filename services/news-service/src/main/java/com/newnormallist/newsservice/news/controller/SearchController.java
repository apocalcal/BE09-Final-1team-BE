package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.NewsListResponse;
import com.newnormallist.newsservice.news.service.NewsService;
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
     * 키워드 검색 (정렬 및 필터링 지원)
     */
    @GetMapping
    public ResponseEntity<Page<NewsListResponse>> searchNews(
            @RequestParam String query,
            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false) String sortOrder,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String press,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            Pageable pageable) {
        Page<NewsListResponse> news = newsService.searchNewsWithFilters(
                query, sortBy, sortOrder, category, press, startDate, endDate, pageable);
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
