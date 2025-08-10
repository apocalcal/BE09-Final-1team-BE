 package com.newsservice.news.controller;

import com.newsservice.news.dto.NewsResponse;
import com.newsservice.news.entity.News;
import com.newsservice.news.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "*")
public class NewsController {
    
    @Autowired
    private NewsService newsService;

    /**
     * 뉴스 개수 조회 API
     * @return 총 뉴스 개수
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getNewsCount() {
        Long count = newsService.getNewsCount();
        return ResponseEntity.ok(count);
    }

    /**
     * 뉴스 조회수 증가
     */
    @PostMapping("/{newsId}/view")
    public ResponseEntity<?> incrementViewCount(@PathVariable Long newsId) {
        newsService.incrementViewCount(newsId);
        return ResponseEntity.ok().build();
    }

    /**
     * 뉴스 목록 조회(페이징 지원)
     */
    @GetMapping
    public ResponseEntity<?> getNews(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword,
            Pageable pageable) {

        try {
            News.Category categoryEntity = null;

            // "전체"는 category 파라미터 생략 처리
            if (category != null && !category.equals("전체")) {
                try {
                    categoryEntity = News.Category.valueOf(category.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException("지원하지 않는 카테고리입니다.");
                }
            }

            Page<NewsResponse> newsList = newsService.getNews(categoryEntity, keyword, pageable);
            return ResponseEntity.ok(newsList);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("지원하지 않는 카테고리입니다.");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("뉴스 조회 중 에러 발생: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("서버 에러가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 특정(단건) 뉴스 상세 조회    
     */
    @GetMapping("/{newsId:[0-9]+}")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable Long newsId) {
        NewsResponse news = newsService.getNewsById(newsId);
        return ResponseEntity.ok(news);
    }
}
