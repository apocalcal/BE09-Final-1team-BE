 package com.newsservice.news.controller;

import com.newsservice.news.dto.CategoryDto;
import com.newsservice.news.dto.NewsListResponse;
import com.newsservice.news.dto.NewsResponse;
import com.newsservice.news.entity.News;
import com.newsservice.news.service.NewsService;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news")
@CrossOrigin(origins = "*")
public class NewsController {
    
    @Autowired
    private NewsService newsService;

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

    // 뉴스 목록 조회(페이징 지원)
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

    // 특정(단건) 뉴스 상세 조회    
    @GetMapping("/{newsId}")
    public ResponseEntity<NewsResponse> getNewsById(@PathVariable Long newsId) {
        NewsResponse news = newsService.getNewsById(newsId);
        return ResponseEntity.ok(news);
    }

    // 개인화 뉴스
    @GetMapping("/personalized")
    public ResponseEntity<List<NewsResponse>> getPersonalizedNews(
            @RequestHeader("X-User-Id") String userId) {
        List<NewsResponse> news = newsService.getPersonalizedNews(Long.parseLong(userId));
        return ResponseEntity.ok(news);
    }

    // 인기 뉴스 (기존 메서드 - List 반환)
    @GetMapping("/trending/list")
    public ResponseEntity<List<NewsResponse>> getTrendingNewsList() {
        List<NewsResponse> news = newsService.getTrendingNews();
        return ResponseEntity.ok(news);
    }

    // 뉴스 조회수 증가
    @PostMapping("/{newsId}/view")
    public ResponseEntity<?> incrementViewCount(@PathVariable Long newsId) {
        newsService.incrementViewCount(newsId);
        return ResponseEntity.ok().build();
    }
    
 
    //  트렌딩 뉴스 (신뢰도 + 조회수 기반)
    @GetMapping("/trending")
    public ResponseEntity<Page<NewsListResponse>> getTrendingNews(Pageable pageable) {
        Page<NewsListResponse> news = newsService.getTrendingNews(pageable);
        return ResponseEntity.ok(news);
    }
    
    //  추천 뉴스 (사용자 기반)
    @GetMapping("/recommendations")
    public ResponseEntity<Page<NewsListResponse>> getRecommendedNews(
            @RequestParam(required = false) Long userId,
            Pageable pageable) {
        if (userId == null) {
            userId = 1L; // 기본 사용자 ID (실제로는 인증된 사용자 ID 사용)
        }
        Page<NewsListResponse> news = newsService.getRecommendedNews(userId, pageable);
        return ResponseEntity.ok(news);
    }
    
    //  카테고리별 뉴스 (카테고리명으로 조회)
    @GetMapping("/category/{categoryName}")
    public ResponseEntity<String> getNewsByCategory(
            @PathVariable String categoryName,
            Pageable pageable) {
        try {
            News.Category category = News.Category.valueOf(categoryName.toUpperCase());
            Page<NewsListResponse> news = newsService.getNewsByCategory(category, pageable);
            return ResponseEntity.ok(String.valueOf(news));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("지원하지 않는 카테고리입니다: " + categoryName);
        }
    }
    
    //  키워드 검색
    @GetMapping("/search")
    public ResponseEntity<Page<NewsListResponse>> searchNews(
            @RequestParam String query,
            Pageable pageable) {
        Page<NewsListResponse> news = newsService.searchNews(query, pageable);
        return ResponseEntity.ok(news);
    }
    
    //  인기 뉴스 (조회수 기반)
    @GetMapping("/popular")
    public ResponseEntity<Page<NewsListResponse>> getPopularNews(Pageable pageable) {
        Page<NewsListResponse> news = newsService.getPopularNews(pageable);
        return ResponseEntity.ok(news);
    }
    
    //  최신 뉴스
    @GetMapping("/latest")
    public ResponseEntity<Page<NewsListResponse>> getLatestNews(Pageable pageable) {
        Page<NewsListResponse> news = newsService.getLatestNews(pageable);
        return ResponseEntity.ok(news);
    }

    /**
     * 언론사별 뉴스 조회 API
     * @param press 언론사명
     * @param pageable 페이징 정보
     * @return 언론사별 뉴스 목록 페이지
     */
    @GetMapping("/press/{press}")
    public ResponseEntity<Page<NewsListResponse>> getNewsByPress(
            @PathVariable String press,
            Pageable pageable) {
        Page<NewsListResponse> news = newsService.getNewsByPress(press, pageable);
        return ResponseEntity.ok(news);
    }

    /**
     * 기간별 뉴스 조회 API
     * @param startDate 시작일시 (형식: yyyy-MM-dd'T'HH:mm:ss)
     * @param endDate 종료일시 (형식: yyyy-MM-dd'T'HH:mm:ss)
     * @return 기간별 뉴스 목록
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<NewsListResponse>> getNewsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        List<NewsListResponse> newsList = newsService.getNewsByDateRange(startDate, endDate);
        return ResponseEntity.ok(newsList);
    }

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
     * 카테고리별 뉴스 개수 조회 API
     * @param categoryName 카테고리명
     * @return 해당 카테고리의 뉴스 개수
     */
    @GetMapping("/count/category/{categoryName}")
    public ResponseEntity<Long> getNewsCountByCategory(@PathVariable String categoryName) {
        try {
            News.Category category = News.Category.valueOf(categoryName.toUpperCase());
            Long count = newsService.getNewsCountByCategory(category);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // 카테고리 목록 조회
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> getCategories() {
        List<CategoryDto> categories = newsService.getAllCategories();
        return ResponseEntity.ok(categories);
    }
    
    // 관리자용: 크롤링된 뉴스를 승격하여 노출용 뉴스로 전환
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
