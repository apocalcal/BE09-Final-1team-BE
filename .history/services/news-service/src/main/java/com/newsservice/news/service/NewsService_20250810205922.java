package com.newsservice.news.service;

import com.newsservice.news.dto.CategoryDto;
import com.newsservice.news.dto.NewsCrawlDto;
import com.newsservice.news.dto.NewsListResponse;
import com.newsservice.news.dto.NewsResponse;
import com.newsservice.news.entity.News;
import com.newsservice.news.entity.NewsCrawl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface NewsService {
    
    // 크롤링된 뉴스 데이터를 저장
    NewsCrawl saveCrawledNews(NewsCrawlDto dto);
    
    // 크롤링된 뉴스 미리보기 (저장하지 않고 미리보기만)
    NewsCrawlDto previewCrawledNews(NewsCrawlDto dto);
    
    // 뉴스 조회 관련 메서드들
    Page<NewsResponse> getNews(News.Category category, String keyword, Pageable pageable);
    NewsResponse getNewsById(Long newsId);
    List<NewsResponse> getPersonalizedNews(Long userId);
    List<NewsResponse> getTrendingNews();
    void incrementViewCount(Long newsId);
    
    // 새로운 API 엔드포인트들을 위한 메서드들
    Page<NewsListResponse> getTrendingNews(Pageable pageable);
    Page<NewsListResponse> getRecommendedNews(Long userId, Pageable pageable);
    Page<NewsListResponse> getNewsByCategory(News.Category category, Pageable pageable);
    Page<NewsListResponse> searchNews(String query, Pageable pageable);
    Page<NewsListResponse> getPopularNews(Pageable pageable);
    Page<NewsListResponse> getLatestNews(Pageable pageable);
    List<CategoryDto> getAllCategories();
    
    // 새로 추가된 메서드들
    Page<NewsListResponse> getNewsByPress(String press, Pageable pageable);
    List<NewsListResponse> getNewsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    Long getNewsCount();
    Long getNewsCountByCategory(News.Category category);
    
    // 관리자용: 크롤링된 뉴스를 승격하여 노출용 뉴스로 전환
    void promoteToNews(Long newsCrawlId);
    
    // 관리자용: 크롤링된 뉴스 목록 조회
    Page<NewsCrawl> getCrawledNews(Pageable pageable);
    
    // === 새로운 고급 검색 기능들 ===
    
    // 고급 검색 (정렬, 필터링 포함)
    Page<NewsListResponse> searchNews(String query, String sortBy, String category, String press, Pageable pageable);
    
    // 자동완성 제안
    List<String> getAutocompleteSuggestions(String query, int limit);
    
    // 인기 검색어
    List<String> getTrendingKeywords(int limit);
    
    // 검색 통계
    Map<String, Object> getSearchStats(String query);
    
    // 관련 키워드 추천
    List<String> getRelatedKeywords(String query, int limit);
    
    // 검색 결과 하이라이팅
    Page<NewsListResponse> searchNewsWithHighlight(String query, Pageable pageable);
    
    // 고급 검색 (여러 조건 조합)
    Page<NewsListResponse> advancedSearch(String query, String category, String press, String reporter, 
                                         LocalDateTime startDate, LocalDateTime endDate, Boolean trusted, 
                                         String sortBy, Pageable pageable);
} 