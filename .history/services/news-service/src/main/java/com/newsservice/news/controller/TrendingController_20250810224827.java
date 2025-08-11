package com.newsservice.news.controller;

import com.newsservice.news.dto.NewsListResponse;
import com.newsservice.news.dto.NewsResponse;
import com.newsservice.news.dto.TrendingKeywordDto;
import com.newsservice.news.service.NewsService;
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
