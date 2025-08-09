package com.newsservice.news.controller;

import com.newsservice.news.dto.NewsCrawlDto;
import com.newsservice.news.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "*")
public class NewsCrawlController {

    @Autowired
    private NewsService newsService;

    // 크롤러에서 전송된 뉴스 데이터를 받아 저장
    @PostMapping("/crawl")
    public ResponseEntity<String> saveCrawledNews(@RequestBody NewsCrawlDto dto) {
        try {
            newsService.saveCrawledNews(dto);
            return ResponseEntity.ok("뉴스가 성공적으로 저장되었습니다.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("저장 실패: " + e.getMessage());
        }
    }
    
    // 크롤링된 뉴스 미리보기 (저장하지 않고 미리보기만)
    @PostMapping("/crawl/preview")
    public ResponseEntity<NewsCrawlDto> previewCrawledNews(@RequestBody NewsCrawlDto dto) {
        NewsCrawlDto preview = newsService.previewCrawledNews(dto);
        return ResponseEntity.ok(preview);
    }
}
