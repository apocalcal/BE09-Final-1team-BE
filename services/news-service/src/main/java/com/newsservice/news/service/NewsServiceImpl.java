package com.newsservice.news.service;

import com.newsservice.news.dto.CategoryDto;
import com.newsservice.news.dto.NewsCrawlDto;
import com.newsservice.news.dto.NewsListResponse;
import com.newsservice.news.dto.NewsResponse;
import com.newsservice.news.entity.Category;
import com.newsservice.news.entity.News;
import com.newsservice.news.entity.NewsCrawl;
import com.newsservice.news.repository.CategoryRepository;
import com.newsservice.news.repository.NewsCrawlRepository;
import com.newsservice.news.repository.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class NewsServiceImpl implements NewsService {

    @Autowired
    private NewsCrawlRepository newsCrawlRepository;
    
    @Autowired
    private NewsRepository newsRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;

    // 크롤링 관련 메서드들
    @Override
    public NewsCrawl saveCrawledNews(NewsCrawlDto dto) {
        // 중복 체크
        if (newsCrawlRepository.existsByLinkId(dto.getLinkId())) {
            throw new RuntimeException("이미 존재하는 뉴스입니다: " + dto.getLinkId());
        }

        // Category 조회
        Category category = null;
        if (dto.getCategoryId() != null) {
            category = categoryRepository.findById(dto.getCategoryId())
                    .orElseThrow(() -> new RuntimeException("존재하지 않는 카테고리입니다: " + dto.getCategoryId()));
        }

        // NewsCrawl 엔티티 생성
        NewsCrawl newsCrawl = NewsCrawl.builder()
                .linkId(dto.getLinkId())
                .title(dto.getTitle())
                .press(dto.getPress())
                .content(dto.getContent())
                .reporterName(dto.getReporterName())
                .publishedAt(dto.getPublishedAt())
                .category(category)
                .createdAt(LocalDateTime.now())
                .build();

        return newsCrawlRepository.save(newsCrawl);
    }

    @Override
    public NewsCrawlDto previewCrawledNews(NewsCrawlDto dto) {
        // 미리보기용으로는 단순히 DTO를 반환 (DB 저장하지 않음)
        return dto;
    }

    // 뉴스 조회 관련 메서드들
    @Override
    public Page<NewsResponse> getNews(Category category, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 키워드 검색
            return newsRepository.searchByKeyword(keyword, pageable)
                    .map(this::convertToNewsResponse);
        } else if (category != null) {
            // 카테고리별 검색
            return newsRepository.findByCategoryId(category.getId(), pageable)
                    .map(this::convertToNewsResponse);
        } else {
            // 전체 뉴스
            return newsRepository.findAll(pageable)
                    .map(this::convertToNewsResponse);
        }
    }
    
    @Override
    public NewsResponse getNewsById(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new RuntimeException("존재하지 않는 뉴스입니다: " + newsId));
        return convertToNewsResponse(news);
    }
    
    @Override
    public List<NewsResponse> getPersonalizedNews(Long userId) {
        // TODO: 사용자 선호도 기반 개인화 로직 구현
        // 현재는 신뢰도가 높은 뉴스 10개 반환
        return newsRepository.findByTrustedGreaterThanEqual(70, Pageable.ofSize(10))
                .getContent()
                .stream()
                .map(this::convertToNewsResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<NewsResponse> getTrendingNews() {
        // 신뢰도가 높은 뉴스 10개 반환
        return newsRepository.findByTrustedGreaterThanEqual(80, Pageable.ofSize(10))
                .getContent()
                .stream()
                .map(this::convertToNewsResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public void incrementViewCount(Long newsId) {
        // TODO: 조회수 증가 로직 구현
        // 현재는 view count 필드가 없으므로 나중에 구현
    }

    // 새로운 API 엔드포인트들을 위한 메서드들
    @Override
    public Page<NewsListResponse> getTrendingNews(Pageable pageable) {
        return newsRepository.findTrendingNews(pageable)
                .map(this::convertToNewsListResponse);
    }
    
    @Override
    public Page<NewsListResponse> getRecommendedNews(Long userId, Pageable pageable) {
        // TODO: 사용자 기반 추천 로직 구현
        // 현재는 신뢰도가 높은 뉴스 반환
        return newsRepository.findByTrustedGreaterThanEqual(70, pageable)
                .map(this::convertToNewsListResponse);
    }
    
    @Override
    public Page<NewsListResponse> getNewsByCategory(Integer categoryId, Pageable pageable) {
        return newsRepository.findByCategoryId(categoryId, pageable)
                .map(this::convertToNewsListResponse);
    }
    
    @Override
    public Page<NewsListResponse> searchNews(String query, Pageable pageable) {
        return newsRepository.searchByKeyword(query, pageable)
                .map(this::convertToNewsListResponse);
    }
    
    @Override
    public Page<NewsListResponse> getPopularNews(Pageable pageable) {
        return newsRepository.findPopularNews(pageable)
                .map(this::convertToNewsListResponse);
    }
    
    @Override
    public Page<NewsListResponse> getLatestNews(Pageable pageable) {
        return newsRepository.findLatestNews(pageable)
                .map(this::convertToNewsListResponse);
    }
    
    @Override
    public List<CategoryDto> getAllCategories() {
        return categoryRepository.findAll()
                .stream()
                .map(this::convertToCategoryDto)
                .collect(Collectors.toList());
    }
    
    @Override
    public void promoteToNews(Long newsCrawlId) {
        // 크롤링된 뉴스를 승격하여 노출용 뉴스로 전환
        NewsCrawl newsCrawl = newsCrawlRepository.findById(newsCrawlId)
                .orElseThrow(() -> new RuntimeException("NewsCrawl not found with id: " + newsCrawlId));
        
        // 이미 승격된 뉴스인지 확인
        List<News> existingNews = newsRepository.findByOriginalNewsId(newsCrawl.getRawId());
        if (!existingNews.isEmpty()) {
            throw new RuntimeException("이미 승격된 뉴스입니다: " + newsCrawlId);
        }
        
        // News 엔티티 생성 및 저장
        News news = News.builder()
                .originalNewsId(newsCrawl.getRawId())
                .publishedAt(newsCrawl.getPublishedAt())
                .summary(generateSummary(newsCrawl.getContent())) // 요약 생성
                .trusted(calculateTrusted(newsCrawl)) // 신뢰도 계산
                .category(newsCrawl.getCategory()) // 카테고리 설정
                .build();
        
        newsRepository.save(news);
    }

    // DTO 변환 메서드들
    private NewsResponse convertToNewsResponse(News news) {
        NewsCrawl originalNews = news.getOriginalNews();
        
        return NewsResponse.builder()
                .newsId(news.getId())
                .originalNewsId(news.getOriginalNewsId())
                .title(originalNews != null ? originalNews.getTitle() : null)
                .content(originalNews != null ? originalNews.getContent() : null)
                .press(originalNews != null ? originalNews.getPress() : null)
                .link(null) // TODO: link 필드 추가 필요
                .summary(news.getSummary())
                .trusted(news.getTrusted())
                .publishedAt(news.getPublishedAt())
                .createdAt(news.getCreatedAt())
                .reporterName(originalNews != null ? originalNews.getReporterName() : null)
                .build();
    }
    
    private NewsListResponse convertToNewsListResponse(News news) {
        NewsCrawl originalNews = news.getOriginalNews();
        
        return NewsListResponse.builder()
                .newsId(news.getId())
                .originalNewsId(news.getOriginalNewsId())
                .title(originalNews != null ? originalNews.getTitle() : null)
                .summary(news.getSummary())
                .press(originalNews != null ? originalNews.getPress() : null)
                .link(null) // TODO: link 필드 추가 필요
                .trusted(news.getTrusted())
                .publishedAt(news.getPublishedAt())
                .createdAt(news.getCreatedAt())
                .reporterName(originalNews != null ? originalNews.getReporterName() : null)
                .viewCount(0) // TODO: view count 필드 추가 필요
                .build();
    }
    
    private CategoryDto convertToCategoryDto(Category category) {
        return CategoryDto.builder()
                .categoryId(category.getId())
                .categoryName(category.getName())
                .displayName(category.getDisplayName())
                .description(category.getDescription())
                .build();
    }
    
    // 요약 생성 메서드 (간단한 구현)
    private String generateSummary(String content) {
        if (content == null || content.length() <= 200) {
            return content;
        }
        return content.substring(0, 200) + "...";
    }
    
    // 신뢰도 계산 메서드 (간단한 구현)
    private Integer calculateTrusted(NewsCrawl newsCrawl) {
        int trusted = 50; // 기본값
        
        // 내용 길이에 따른 신뢰도 조정
        if (newsCrawl.getContent() != null) {
            if (newsCrawl.getContent().length() > 1000) {
                trusted += 20;
            } else if (newsCrawl.getContent().length() > 500) {
                trusted += 10;
            }
        }
        
        // 기자명이 있는 경우 신뢰도 증가
        if (newsCrawl.getReporterName() != null && !newsCrawl.getReporterName().trim().isEmpty()) {
            trusted += 10;
        }
        
        // 언론사에 따른 신뢰도 조정
        if (newsCrawl.getPress() != null) {
            String press = newsCrawl.getPress().toLowerCase();
            if (press.contains("조선일보") || press.contains("중앙일보") || press.contains("동아일보")) {
                trusted += 15;
            } else if (press.contains("한겨레") || press.contains("경향신문")) {
                trusted += 10;
            }
        }
        
        return Math.min(trusted, 100); // 최대 100
    }
} 