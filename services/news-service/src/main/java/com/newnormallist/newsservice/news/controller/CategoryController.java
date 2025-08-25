package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.CategoryDto;
import com.newnormallist.newsservice.news.dto.NewsListResponse;
import com.newnormallist.newsservice.news.entity.Category;
import com.newnormallist.newsservice.news.service.NewsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {
    
    @Autowired
    private NewsService newsService;

    /**
     * 카테고리 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<CategoryDto>> getCategories() {
        List<CategoryDto> categories = newsService.getAllCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * 카테고리별 뉴스 조회
     */
    @GetMapping("/{categoryName}/news")
    public ResponseEntity<?> getNewsByCategory(
            @PathVariable String categoryName,
            Pageable pageable) {
        try {
            Category category = Category.valueOf(categoryName.toUpperCase());
            Page<NewsListResponse> news = newsService.getNewsByCategory(category, pageable);
            return ResponseEntity.ok(news);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("지원하지 않는 카테고리입니다: " + categoryName + 
                ". 사용 가능한 카테고리: POLITICS, ECONOMY, SOCIETY, LIFE, INTERNATIONAL, IT_SCIENCE");
        }
    }

    /**
     * 카테고리별 뉴스 개수 조회
     */
    @GetMapping("/{categoryName}/count")
    public ResponseEntity<?> getNewsCountByCategory(@PathVariable String categoryName) {
        try {
            Category category = Category.valueOf(categoryName.toUpperCase());
            Long count = newsService.getNewsCountByCategory(category);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("지원하지 않는 카테고리입니다: " + categoryName + 
                ". 사용 가능한 카테고리: POLITICS, ECONOMY, SOCIETY, LIFE, INTERNATIONAL, IT_SCIENCE");
        }
    }
}
