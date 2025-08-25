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
import com.newnormallist.newsservice.news.entity.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/trending")
@CrossOrigin(origins = "*")
public class TrendingController {
    
    private static final Logger log = LoggerFactory.getLogger(TrendingController.class);

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

    /**
     * 카테고리별 트렌딩 키워드 조회
     */
    @GetMapping("/trending-keywords/category/{categoryName}")
    public ResponseEntity<ApiResponse<List<TrendingKeywordDto>>> getTrendingKeywordsByCategory(
            @PathVariable("categoryName") String categoryName,
            @RequestParam(defaultValue = "8") int limit,
            @RequestParam(defaultValue = "24") int hours
    ) {
        try {
            Category category = Category.valueOf(categoryName.toUpperCase());
            List<TrendingKeywordDto> result = newsService.getTrendingKeywordsByCategory(category, limit);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                .body(ApiResponse.fail("유효하지 않은 카테고리입니다: " + categoryName));
        } catch (Exception e) {
            log.error("카테고리별 트렌딩 키워드 조회 실패: category={}", categoryName, e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.fail("트렌딩 키워드 조회 중 오류가 발생했습니다."));
        }
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
