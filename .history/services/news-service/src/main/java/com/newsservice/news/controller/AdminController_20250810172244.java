package com.newsservice.news.controller;

import com.newsservice.news.entity.NewsCrawl;
import com.newsservice.news.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@Component
public class AdminController {
    
    @Autowired
    private NewsService newsService;

    /**
     * 관리자용: 크롤링된 뉴스 목록 조회
     */
    @GetMapping("/crawled-news")
    public ResponseEntity<Page<NewsCrawl>> getCrawledNews(Pageable pageable) {
        Page<NewsCrawl> newsList = newsService.getCrawledNews(pageable);
        return ResponseEntity.ok(newsList);
    }

    /**
     * 관리자용: 크롤링된 뉴스를 승격하여 노출용 뉴스로 전환
     */
    @PostMapping("/promote/{newsCrawlId}")
    public ResponseEntity<String> promoteNews(@PathVariable Long newsCrawlId) {
        try {
            newsService.promoteToNews(newsCrawlId);
            return ResponseEntity.ok("뉴스가 성공적으로 승격되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("승격 실패: " + e.getMessage());
        }
    }
}
