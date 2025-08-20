package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.NewsListResponse;
import com.newnormallist.newsservice.news.dto.NewsResponse;
import com.newnormallist.newsservice.news.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/personalization")
@CrossOrigin(origins = "*")
public class PersonalizationController {
    
    @Autowired
    private NewsService newsService;

    /**
     * 개인화 뉴스
     */
    @GetMapping("/news")
    public ResponseEntity<List<NewsResponse>> getPersonalizedNews(
            @RequestHeader("X-User-Id") String userId) {
        List<NewsResponse> news = newsService.getPersonalizedNews(Long.parseLong(userId));
        return ResponseEntity.ok(news);
    }

    /**
     * 추천 뉴스 (사용자 기반)
     */
    @GetMapping("/recommendations")
    public ResponseEntity<Page<NewsListResponse>> getRecommendedNews(
            @RequestParam(required = false) Long userId,
            Pageable pageable) {
        if (userId == null) {
            userId = 1L; // 기본 사용자 ID (실제로는 인증된 사용자 ID 사용)
        }
        Page<NewsListResponse> news = newsService.getRecommendedNews(userId, pageable);
        return ResponseEntity.ok(news);
    }
}
