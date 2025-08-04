package com.newsservice.news.service;

import com.newsservice.news.dto.CategoryDto;
import com.newsservice.news.dto.NewsCrawlDto;
import com.newsservice.news.dto.NewsListResponse;
import com.newsservice.news.dto.NewsResponse;
import com.newsservice.news.entity.Category;
import com.newsservice.news.entity.NewsCrawl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
    Page<NewsListResponse> getNewsByCategory(Integer categoryId, Pageable pageable);
    Page<NewsListResponse> searchNews(String query, Pageable pageable);
    Page<NewsListResponse> getPopularNews(Pageable pageable);
    Page<NewsListResponse> getLatestNews(Pageable pageable);
    List<CategoryDto> getAllCategories();
    
    // 관리자용: 크롤링된 뉴스를 승격하여 노출용 뉴스로 전환
    void promoteToNews(Long newsCrawlId);
} 