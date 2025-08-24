package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.NewsListResponse;
import com.newnormallist.newsservice.news.dto.NewsResponse;
import com.newnormallist.newsservice.news.dto.TrendingKeywordDto;
import com.newnormallist.newsservice.news.service.NewsService;
import com.newnormallist.newsservice.news.service.TrendingService;
import com.newnormallist.newsservice.recommendation.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trending")
@CrossOrigin(origins = "*")
public class TrendingController {
    
    @Autowired
    private NewsService newsService;
    @Autowired
    private TrendingService trendingService;

    /**
     * 트렌딩 뉴스 (페이징)
     */
    @GetMapping
    public ResponseEntity<Page<NewsListResponse>> getTrendingNews(Pageable pageable) {
        Page<NewsListResponse> news = newsService.getTrendingNews(pageable);
        return ResponseEntity.ok(news);
    }

    /**
     * 트렌딩 뉴스 (리스트)
     */
    @GetMapping("/list")
    public ResponseEntity<List<NewsResponse>> getTrendingNewsList() {
        List<NewsResponse> news = newsService.getTrendingNews();
        return ResponseEntity.ok(news);
    }

    /**
     * 실시간 인기 키워드 조회
     */
    @GetMapping("/trending-keywords")
    public ResponseEntity<ApiResponse<List<TrendingKeywordDto>>> getTrendingKeywords(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "24h") String period,
            @RequestParam(required = false) Integer hours
    ) {
        int windowHours = (hours != null) ? hours : parsePeriodToHours(period);
        List<TrendingKeywordDto> result = trendingService.getTrendingKeywords(windowHours, limit);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    private int parsePeriodToHours(String period) {
        try {
            String p = period.trim().toLowerCase();
            if (p.endsWith("h")) return Integer.parseInt(p.substring(0, p.length()-1));
            if (p.endsWith("m")) return Math.max(1, Integer.parseInt(p.substring(0, p.length()-1)) / 60);
            return Integer.parseInt(p);
        } catch (Exception e) {
            return 24;
        }
    }



    /**
     * 인기 뉴스 (조회수 기반)
     */
    @GetMapping("/popular")
    public ResponseEntity<Page<NewsListResponse>> getPopularNews(Pageable pageable) {
        Page<NewsListResponse> news = newsService.getPopularNews(pageable);
        return ResponseEntity.ok(news);
    }

    /**
     * 최신 뉴스
     */
    @GetMapping("/latest")
    public ResponseEntity<Page<NewsListResponse>> getLatestNews(Pageable pageable) {
        Page<NewsListResponse> news = newsService.getLatestNews(pageable);
        return ResponseEntity.ok(news);
    }

    /**
     * 트렌딩 키워드 조회
     */
    @GetMapping("/keywords")
    public ResponseEntity<List<TrendingKeywordDto>> getTrendingKeywords(
            @RequestParam(defaultValue = "10") int limit) {
        List<TrendingKeywordDto> keywords = newsService.getTrendingKeywords(limit);
        return ResponseEntity.ok(keywords);
    }

    /**
     * 인기 키워드 조회
     */
    @GetMapping("/keywords/popular")
    public ResponseEntity<List<TrendingKeywordDto>> getPopularKeywords(
            @RequestParam(defaultValue = "10") int limit) {
        List<TrendingKeywordDto> keywords = newsService.getPopularKeywords(limit);
        return ResponseEntity.ok(keywords);
    }
}
