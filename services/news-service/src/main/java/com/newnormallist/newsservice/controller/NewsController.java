package com.newnormallist.newsservice.controller;

import com.newnormallist.newsservice.dto.NewsDetailResponseDto;
import com.newnormallist.newsservice.dto.NewsListResponseDto;
import com.newnormallist.newsservice.entity.News;
import com.newnormallist.newsservice.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*") // 개발 단계에서는 모든 도메인 허용
public class NewsController {

    private final NewsService newsService;

    /**
     * 헬스 체크 API
     */
    @GetMapping("/health")
    public ResponseEntity<String> healthCheck() {
        return ResponseEntity.ok("News Service is running");
    }

    /**
     * 뉴스 상세 정보 조회 API
     * @param newsId 뉴스 ID
     * @return 뉴스 상세 정보
     */
    @GetMapping("/{newsId}")
    public ResponseEntity<NewsDetailResponseDto> getNewsDetail(@PathVariable Long newsId) {
        log.info("뉴스 상세 정보 조회 요청 - newsId: {}", newsId);

        NewsDetailResponseDto newsDetail = newsService.getNewsDetail(newsId);
        return ResponseEntity.ok(newsDetail);
    }

    /**
     * 모든 뉴스 목록 조회 API (페이징)
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 뉴스 목록 페이지
     */
    @GetMapping
    public ResponseEntity<Page<NewsListResponseDto>> getAllNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        log.info("뉴스 목록 조회 요청 - page: {}, size: {}", page, size);
        
        Page<NewsListResponseDto> newsPage = newsService.getAllNews(page, size);
        return ResponseEntity.ok(newsPage);
    }

    /**≈
     * 카테고리별 뉴스 목록 조회 API
     * @param categoryName 카테고리명
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 카테고리별 뉴스 목록 페이지
     */
    @GetMapping("/category/{categoryName}")
    public ResponseEntity<Page<NewsListResponseDto>> getNewsByCategory(
            @PathVariable String categoryName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("카테고리별 뉴스 조회 요청 - category: {}, page: {}, size: {}", categoryName, page, size);

        try {
            News.CategoryType categoryType = News.CategoryType.valueOf(categoryName.toUpperCase());
            Page<NewsListResponseDto> newsPage = newsService.getNewsByCategory(categoryType, page, size);
            return ResponseEntity.ok(newsPage);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 카테고리명: {}", categoryName);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 뉴스 제목 검색 API
     * @param keyword 검색 키워드
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 검색된 뉴스 목록 페이지
     */
    @GetMapping("/search")
    public ResponseEntity<Page<NewsListResponseDto>> searchNews(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("뉴스 검색 요청 - keyword: {}, page: {}, size: {}", keyword, page, size);

        Page<NewsListResponseDto> newsPage = newsService.searchNewsByTitle(keyword, page, size);
        return ResponseEntity.ok(newsPage);
    }

    /**
     * 언론사별 뉴스 조회 API
     * @param press 언론사명
     * @param page 페이지 번호 (기본값: 0)
     * @param size 페이지 크기 (기본값: 20)
     * @return 언론사별 뉴스 목록 페이지
     */
    @GetMapping("/press/{press}")
    public ResponseEntity<Page<NewsListResponseDto>> getNewsByPress(
            @PathVariable String press,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("언론사별 뉴스 조회 요청 - press: {}, page: {}, size: {}", press, page, size);

        Page<NewsListResponseDto> newsPage = newsService.getNewsByPress(press, page, size);
        return ResponseEntity.ok(newsPage);
    }

    /**
     * 최신 뉴스 조회 API
     * @param limit 조회할 뉴스 개수 (기본값: 10)
     * @return 최신 뉴스 목록
     */
    @GetMapping("/latest")
    public ResponseEntity<List<NewsListResponseDto>> getLatestNews(
            @RequestParam(defaultValue = "10") int limit) {
        log.info("최신 뉴스 조회 요청 - limit: {}", limit);

        List<NewsListResponseDto> newsList = newsService.getLatestNews(limit);
        return ResponseEntity.ok(newsList);
    }

    /**
     * 기간별 뉴스 조회 API
     * @param startDate 시작일시 (형식: yyyy-MM-dd'T'HH:mm:ss)
     * @param endDate 종료일시 (형식: yyyy-MM-dd'T'HH:mm:ss)
     * @return 기간별 뉴스 목록
     */
    @GetMapping("/date-range")
    public ResponseEntity<List<NewsListResponseDto>> getNewsByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        log.info("기간별 뉴스 조회 요청 - startDate: {}, endDate: {}", startDate, endDate);

        List<NewsListResponseDto> newsList = newsService.getNewsByDateRange(startDate, endDate);
        return ResponseEntity.ok(newsList);
    }

    /**
     * 뉴스 개수 조회 API
     * @return 총 뉴스 개수
     */
    @GetMapping("/count")
    public ResponseEntity<Long> getNewsCount() {
        log.info("뉴스 개수 조회 요청");
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
        log.info("카테고리별 뉴스 개수 조회 요청 - category: {}", categoryName);

        try {
            News.CategoryType categoryType = News.CategoryType.valueOf(categoryName.toUpperCase());
            Long count = newsService.getNewsCountByCategory(categoryType);
            return ResponseEntity.ok(count);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 카테고리명: {}", categoryName);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 중복제거 상태별 뉴스 조회 API
     * @param dedupState 중복제거 상태 (ORIGINAL, DUPLICATE, PROCESSED)
     * @return 중복제거 상태별 뉴스 목록
     */
    @GetMapping("/dedup/{dedupState}")
    public ResponseEntity<List<NewsListResponseDto>> getNewsByDedupState(@PathVariable String dedupState) {
        log.info("중복제거 상태별 뉴스 조회 요청 - dedupState: {}", dedupState);

        try {
            News.DedupState state = News.DedupState.valueOf(dedupState.toUpperCase());
            List<NewsListResponseDto> newsList = newsService.getNewsByDedupState(state);
            return ResponseEntity.ok(newsList);
        } catch (IllegalArgumentException e) {
            log.warn("잘못된 중복제거 상태: {}", dedupState);
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 지원하는 카테고리 목록 조회 API
     * @return 카테고리 목록
     */
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryInfo>> getCategories() {
        log.info("카테고리 목록 조회 요청");

        List<CategoryInfo> categories = List.of(
                new CategoryInfo("POLITICS", "정치"),
                new CategoryInfo("ECONOMY", "경제"),
                new CategoryInfo("SOCIETY", "사회"),
                new CategoryInfo("CULTURE", "문화"),
                new CategoryInfo("INTERNATIONAL", "국제"),
                new CategoryInfo("IT", "IT")
        );

        return ResponseEntity.ok(categories);
    }

    /**
     * 카테고리 정보 내부 클래스
     */
    public static class CategoryInfo {
        public String code;
        public String name;

        public CategoryInfo(String code, String name) {
            this.code = code;
            this.name = name;
        }
    }
}
