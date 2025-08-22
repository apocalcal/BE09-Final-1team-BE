package com.newnormallist.newsservice.recommendation.controller;

import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import lombok.RequiredArgsConstructor;
import com.newnormallist.newsservice.recommendation.service.RecommendationService;
import com.newnormallist.newsservice.recommendation.dto.ApiResponse;
import com.newnormallist.newsservice.recommendation.dto.FeedItemDto;
import java.util.List;

// RecommendationService를 호출해 최종 피드(뉴스 리스트) DTO로 반환
// 첫 페이지: 개인화 추천, 나머지 페이지: 전체 뉴스 최신순
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/feed")
public class FeedController {

    private final RecommendationService recommendationService;

    @GetMapping("/users/{userId}/feed")
    public ApiResponse<List<FeedItemDto>> getFeed(@PathVariable Long userId) {
        return ApiResponse.success(recommendationService.getFeed(userId));
    }
}