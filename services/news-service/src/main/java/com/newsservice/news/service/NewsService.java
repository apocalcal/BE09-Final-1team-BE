package com.newsservice.news.service;

import com.newsservice.news.dto.CategoryDto;
import com.newsservice.news.dto.KeywordSubscriptionDto;
import com.newsservice.news.dto.NewsCrawlDto;
import com.newsservice.news.dto.NewsListResponse;
import com.newsservice.news.dto.NewsResponse;
import com.newsservice.news.dto.TrendingKeywordDto;
import com.newsservice.news.entity.Category;
import com.newsservice.news.entity.News;
import com.newsservice.news.entity.NewsCrawl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface NewsService {
    
    // 크롤링된 뉴스 데이터를 저장
    NewsCrawl saveCrawledNews(NewsCrawlDto dto);
    
    // 크롤링된 뉴스 미리보기 (저장하지 않고 미리보기만)
    NewsCrawlDto previewCrawledNews(NewsCrawlDto dto);
    
    // 뉴스 조회 관련 메서드들
    Page<NewsResponse> getNews(Category category, String keyword, Pageable pageable);
    NewsResponse getNewsById(Long newsId);
    List<NewsResponse> getPersonalizedNews(Long userId);
    List<NewsResponse> getTrendingNews();
    void incrementViewCount(Long newsId);
    
    // 새로운 API 엔드포인트들을 위한 메서드들
    Page<NewsListResponse> getTrendingNews(Pageable pageable);
    Page<NewsListResponse> getRecommendedNews(Long userId, Pageable pageable);
    Page<NewsListResponse> getNewsByCategory(Category category, Pageable pageable);
    Page<NewsListResponse> searchNews(String query, Pageable pageable);
    Page<NewsListResponse> searchNewsWithFilters(String query, String sortBy, String sortOrder, 
                                                String category, String press, String startDate, 
                                                String endDate, Pageable pageable);
    Page<NewsListResponse> getPopularNews(Pageable pageable);
    Page<NewsListResponse> getLatestNews(Pageable pageable);
    List<CategoryDto> getAllCategories();
    
    // 새로 추가된 메서드들
    Page<NewsListResponse> getNewsByPress(String press, Pageable pageable);
    List<NewsListResponse> getNewsByDateRange(LocalDateTime startDate, LocalDateTime endDate);
    Long getNewsCount();
    Long getNewsCountByCategory(Category category);
    
    // 관리자용: 크롤링된 뉴스를 승격하여 노출용 뉴스로 전환
    void promoteToNews(Long newsCrawlId);
    
    // 관리자용: 크롤링된 뉴스 목록 조회
    Page<NewsCrawl> getCrawledNews(Pageable pageable);
    
    // 키워드 구독 관련 메서드들
    KeywordSubscriptionDto subscribeKeyword(Long userId, String keyword);
    void unsubscribeKeyword(Long userId, String keyword);
    List<KeywordSubscriptionDto> getUserKeywordSubscriptions(Long userId);
    
    // 트렌딩 키워드 관련 메서드들
    List<TrendingKeywordDto> getTrendingKeywords(int limit);
    List<TrendingKeywordDto> getPopularKeywords(int limit);
} 