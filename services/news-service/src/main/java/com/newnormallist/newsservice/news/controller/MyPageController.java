package com.newsservice.news.controller;

import com.newsservice.news.dto.NewsListResponse;
import com.newsservice.news.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping("/scraps")
    public ResponseEntity<?> getMyScraps(
            @AuthenticationPrincipal String userIdString,
            @RequestParam(required = false) String category,
            Pageable pageable) {
        if (userIdString == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("사용자 인증 정보가 없습니다.");
        }
        Long userId = Long.parseLong(userIdString);
        Page<NewsListResponse> scraps = myPageService.getScrappedNews(userId, category, pageable);
        return ResponseEntity.ok(scraps);
    }

    @DeleteMapping("/scraps/{newsId}")
    public ResponseEntity<Void> deleteScrap(
            @AuthenticationPrincipal String userIdString,
            @PathVariable Long newsId) {
        if (userIdString == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Long userId = Long.parseLong(userIdString);
        myPageService.deleteScrap(userId, newsId);
        return ResponseEntity.noContent().build();
    }
}