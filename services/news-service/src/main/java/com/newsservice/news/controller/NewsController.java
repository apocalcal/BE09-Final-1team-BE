package com.newsservice.news.controller;

import com.newsservice.news.dto.NewsListResponse;
import com.newsservice.news.dto.NewsResponse;
import com.newsservice.news.entity.Category;
import com.newsservice.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NewsController {

    private final NewsService newsService;

    // --- 기존 코드 (변경 없음) ---
    @GetMapping("/count")
    public ResponseEntity<Long> getNewsCount() {
        return ResponseEntity.ok(newsService.getNewsCount());
    }

    @PostMapping("/{newsId}/view")
    public ResponseEntity<?> incrementViewCount(@PathVariable Long newsId) {
        newsService.incrementViewCount(newsId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public ResponseEntity<?> getNews(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {

        try {
            Category categoryEntity = null;
            if (category != null && !category.equals("전체")) {
                try {
                    categoryEntity = Category.valueOf(category.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body("지원하지 않는 카테고리입니다.");
                }
            }
            Page<NewsResponse> newsList = newsService.getNews(categoryEntity, keyword, pageable);
            return ResponseEntity.ok(newsList);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 에러가 발생했습니다: " + e.getMessage());
        }
    }

    @GetMapping("/{newsId:[0-9]+}")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable Long newsId) {
        return ResponseEntity.ok(newsService.getNewsById(newsId));
    }

    @PostMapping("/{newsId}/report")
    public ResponseEntity<?> reportNews(@PathVariable Long newsId, @AuthenticationPrincipal String userIdString) {
        if (userIdString == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("사용자 인증 정보가 없습니다.");
        }
        Long userId = Long.parseLong(userIdString);
        newsService.reportNews(newsId, userId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{newsId}/scrap")
    public ResponseEntity<?> scrapNews(@PathVariable Long newsId, @AuthenticationPrincipal String userIdString) {
        if (userIdString == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("사용자 인증 정보가 없습니다.");
        }
        Long userId = Long.parseLong(userIdString);
        newsService.scrapNews(newsId, userId);
        return ResponseEntity.ok().build();
    }
}
