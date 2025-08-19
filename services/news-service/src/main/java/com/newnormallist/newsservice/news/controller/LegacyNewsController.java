package com.newnormallist.newsservice.news.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "*")
@Deprecated
public class LegacyNewsController {
    
    @Autowired
    private SystemController systemController;
    
    @Autowired
    private CategoryController categoryController;
    
    @Autowired
    private TrendingController trendingController;
    
    @Autowired
    private PersonalizationController personalizationController;
    
    @Autowired
    private SearchController searchController;
    
    @Autowired
    private AdminController adminController;

    // 기존 URL들을 새 컨트롤러로 위임 (Deprecated 경고와 함께)
    
    @GetMapping("/health")
    @Deprecated
    public ResponseEntity<String> healthCheck() {
        return systemController.healthCheck();
    }

    @GetMapping("/test-db")
    @Deprecated
    public ResponseEntity<String> databaseTest() {
        return systemController.databaseTest();
    }

    @GetMapping("/categories")
    @Deprecated
    public ResponseEntity<?> getCategories() {
        return categoryController.getCategories();
    }

    @GetMapping("/category/{categoryName}")
    @Deprecated
    public ResponseEntity<?> getNewsByCategory(@PathVariable String categoryName, Pageable pageable) {
        return categoryController.getNewsByCategory(categoryName, pageable);
    }

    @GetMapping("/trending")
    @Deprecated
    public ResponseEntity<?> getTrendingNews(Pageable pageable) {
        return trendingController.getTrendingNews(pageable);
    }

    @GetMapping("/trending/list")
    @Deprecated
    public ResponseEntity<?> getTrendingNewsList() {
        return trendingController.getTrendingNewsList();
    }

    @GetMapping("/popular")
    @Deprecated
    public ResponseEntity<?> getPopularNews(Pageable pageable) {
        return trendingController.getPopularNews(pageable);
    }

    @GetMapping("/latest")
    @Deprecated
    public ResponseEntity<?> getLatestNews(Pageable pageable) {
        return trendingController.getLatestNews(pageable);
    }

    @GetMapping("/personalized")
    @Deprecated
    public ResponseEntity<?> getPersonalizedNews(@RequestHeader("X-User-Id") String userId) {
        return personalizationController.getPersonalizedNews(userId);
    }

    @GetMapping("/recommendations")
    @Deprecated
    public ResponseEntity<?> getRecommendedNews(@RequestParam(required = false) Long userId, Pageable pageable) {
        return personalizationController.getRecommendedNews(userId, pageable);
    }

    @GetMapping("/search")
    @Deprecated
    public ResponseEntity<?> searchNews(@RequestParam String query, Pageable pageable) {
        return searchController.searchNews(query, null, null, null, null, null, null, pageable);
    }

    @GetMapping("/press/{press}")
    @Deprecated
    public ResponseEntity<?> getNewsByPress(@PathVariable String press, Pageable pageable) {
        return searchController.getNewsByPress(press, pageable);
    }

    @PostMapping("/promote/{newsCrawlId}")
    @Deprecated
    public ResponseEntity<String> promoteNews(@PathVariable Long newsCrawlId) {
        return adminController.promoteNews(newsCrawlId);
    }
}
