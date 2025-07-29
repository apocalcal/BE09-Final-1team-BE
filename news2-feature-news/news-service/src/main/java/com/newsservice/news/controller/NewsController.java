package com.newsservice.news.controller;

import com.newsservice.news.dto.NewsResponse;
import com.newsservice.news.entity.NewsCategory;
import com.newsservice.news.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "*")
public class NewsController {
    
    @Autowired
    private NewsService newsService;
    
    @GetMapping
    public ResponseEntity<Page<NewsResponse>> getNews(
            @RequestParam(required = false) NewsCategory category,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {
        Page<NewsResponse> news = newsService.getNews(category, keyword, pageable);
        return ResponseEntity.ok(news);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable Long id) {
        NewsResponse news = newsService.getNewsById(id);
        return ResponseEntity.ok(news);
    }
    
    @GetMapping("/personalized")
    public ResponseEntity<List<NewsResponse>> getPersonalizedNews(
            @RequestHeader("X-User-Id") String userId) {
        List<NewsResponse> news = newsService.getPersonalizedNews(Long.parseLong(userId));
        return ResponseEntity.ok(news);
    }
    
    @GetMapping("/trending")
    public ResponseEntity<List<NewsResponse>> getTrendingNews() {
        List<NewsResponse> news = newsService.getTrendingNews();
        return ResponseEntity.ok(news);
    }
    
    @PostMapping("/{id}/view")
    public ResponseEntity<?> incrementViewCount(@PathVariable Long id) {
        newsService.incrementViewCount(id);
        return ResponseEntity.ok().build();
    }
}
