package com.newnormalist.newsservice.news.controller;

import com.newnormalist.newsservice.news.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/system")
@CrossOrigin(origins = "*")
public class SystemController {
    
    @Autowired
    private NewsService newsService;

    /**
     * 헬스 체크 API
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("News Service is running");
    }

    /**
     * 데이터베이스 연결 테스트 API
     */
    @GetMapping("/test-db")
    public ResponseEntity<String> databaseTest() {
        try {
            long count = newsService.getNewsCount();
            return ResponseEntity.ok("데이터베이스 연결 성공. 뉴스 개수: " + count);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("데이터베이스 연결 실패: " + e.getMessage());
        }
    }
}
