package com.newsservice.news.controller;

import com.newsservice.news.dto.KeywordSubscriptionDto;
import com.newsservice.news.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/keywords")
@CrossOrigin(origins = "*")
public class KeywordSubscriptionController {
    
    @Autowired
    private NewsService newsService;

    /**
     * 키워드 구독
     */
    @PostMapping("/subscribe")
    public ResponseEntity<KeywordSubscriptionDto> subscribeKeyword(
            @RequestParam Long userId,
            @RequestParam String keyword) {
        try {
            KeywordSubscriptionDto subscription = newsService.subscribeKeyword(userId, keyword);
            return ResponseEntity.ok(subscription);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 키워드 구독 해제
     */
    @DeleteMapping("/unsubscribe")
    public ResponseEntity<?> unsubscribeKeyword(
            @RequestParam Long userId,
            @RequestParam String keyword) {
        try {
            newsService.unsubscribeKeyword(userId, keyword);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 사용자의 키워드 구독 목록 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<KeywordSubscriptionDto>> getUserKeywordSubscriptions(
            @PathVariable Long userId) {
        List<KeywordSubscriptionDto> subscriptions = newsService.getUserKeywordSubscriptions(userId);
        return ResponseEntity.ok(subscriptions);
    }
}
