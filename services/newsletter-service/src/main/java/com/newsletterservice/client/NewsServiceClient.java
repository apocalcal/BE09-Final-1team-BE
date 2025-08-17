package com.newsletterservice.client;

import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.client.dto.NewsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "news-service", url = "${services.news-service.url:http://localhost:8083}")
public interface NewsServiceClient {
    
    /**
     * 최신 뉴스 조회
     */
    @GetMapping("/api/v1/news/latest")
    ApiResponse<List<NewsResponse>> getLatestNews(
            @RequestParam(required = false) List<String> categories,
            @RequestParam(defaultValue = "10") int limit
    );
    
    /**
     * 카테고리별 뉴스 조회
     */
    @GetMapping("/api/v1/news/category/{category}")
    ApiResponse<List<NewsResponse>> getNewsByCategory(
            @PathVariable("category") String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );
    
    /**
     * 트렌딩 뉴스 조회
     */
    @GetMapping("/api/v1/news/trending")
    ApiResponse<List<NewsResponse>> getTrendingNews(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "5") int limit
    );
    
    /**
     * 키워드 기반 뉴스 검색
     */
    @GetMapping("/api/v1/news/search")
    ApiResponse<List<NewsResponse>> searchNews(
            @RequestParam("keyword") String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    );
    
    /**
     * 뉴스 상세 정보 조회
     */
    @GetMapping("/api/v1/news/{newsId}")
    ApiResponse<NewsResponse> getNewsById(@PathVariable("newsId") Long newsId);
    
    /**
     * 여러 뉴스 일괄 조회
     */
    @PostMapping("/api/v1/news/batch")
    ApiResponse<List<NewsResponse>> getNewsByIds(@RequestBody List<Long> newsIds);
    
//    /**
//     * 카테고리 목록 조회
//     */
//    @GetMapping("/api/v1/categories")
//    ApiResponse<List<CategoryDto>> getCategories();
}
