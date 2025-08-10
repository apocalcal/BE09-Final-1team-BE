package com.newsservice.news.service;

import com.newsservice.news.dto.CategoryDto;
import com.newsservice.news.dto.NewsCrawlDto;
import com.newsservice.news.dto.NewsListResponse;
import com.newsservice.news.dto.NewsResponse;
import com.newsservice.news.entity.News;
import com.newsservice.news.entity.NewsCrawl;
import com.newsservice.news.exception.NewsNotFoundException;

import com.newsservice.news.repository.NewsCrawlRepository;
import com.newsservice.news.repository.NewsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@Service
@Transactional
public class NewsServiceImpl implements NewsService {

    @Autowired
    private NewsCrawlRepository newsCrawlRepository;
    
    @Autowired
    private NewsRepository newsRepository;
    


    // 크롤링 관련 메서드들
    @Override
    public NewsCrawl saveCrawledNews(NewsCrawlDto dto) {
        // 중복 체크
        if (newsCrawlRepository.existsByLinkId(dto.getLinkId())) {
            throw new RuntimeException("이미 존재하는 뉴스입니다: " + dto.getLinkId());
        }

        // Category enum 사용
        News.Category category = dto.getCategory();

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
    public Page<NewsResponse> getNews(News.Category category, String keyword, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            // 키워드 검색
            return newsRepository.searchByKeyword(keyword, pageable)
                    .map(this::convertToNewsResponse);
        } else if (category != null) {
            // 카테고리별 검색
            return newsRepository.findByCategory(category, pageable)
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
                .orElseThrow(() -> new NewsNotFoundException("존재하지 않는 뉴스입니다: " + newsId));
        return convertToNewsResponse(news);
    }
    
    @Override
    public List<NewsResponse> getPersonalizedNews(Long userId) {
        // TODO: 사용자 선호도 기반 개인화 로직 구현
        // 현재는 신뢰도가 높은 뉴스 10개 반환
        return newsRepository.findByTrustedTrue(Pageable.ofSize(10))
                .getContent()
                .stream()
                .map(this::convertToNewsResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<NewsResponse> getTrendingNews() {
        // 신뢰도가 높은 뉴스 10개 반환
        return newsRepository.findByTrustedTrue(Pageable.ofSize(10))
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
        return newsRepository.findByTrustedTrue(pageable)
                .map(this::convertToNewsListResponse);
    }
    
    @Override
    public Page<NewsListResponse> getNewsByCategory(News.Category category, Pageable pageable) {
        return newsRepository.findByCategory(category, pageable)
                .map(this::convertToNewsListResponse);
    }
    
    @Override
    public Page<NewsListResponse> searchNews(String query, Pageable pageable) {
        return newsRepository.searchByKeyword(query, pageable)
                .map(this::convertToNewsListResponse);
    }
    
    // 새로운 고급 검색 메서드 (오버로드)
    @Override
    public Page<NewsListResponse> searchNews(String query, String sortBy, String category, String press, Pageable pageable) {
        News.Category categoryEnum = null;
        if (category != null && !category.isEmpty()) {
            try {
                categoryEnum = News.Category.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 잘못된 카테고리명은 무시
            }
        }
        
        Page<News> newsPage = newsRepository.searchByKeywordAdvanced(query, categoryEnum, press, pageable);
        
        // 정렬 옵션에 따른 추가 처리
        if ("popular".equals(sortBy)) {
            newsPage = newsRepository.findPopularNews(pageable);
        } else if ("latest".equals(sortBy)) {
            newsPage = newsRepository.findLatestNews(pageable);
        }
        
        return newsPage.map(this::convertToNewsListResponse);
    }
    
    @Override
    public List<String> getAutocompleteSuggestions(String query, int limit) {
        if (query == null || query.trim().isEmpty() || query.length() < 2) {
            return List.of();
        }
        
        Pageable pageable = Pageable.ofSize(limit);
        List<String> titles = newsRepository.findTitlesByKeyword(query, pageable);
        
        // 제목에서 키워드 추출 및 정리
        return titles.stream()
                .filter(title -> title != null && !title.isEmpty())
                .map(title -> {
                    // 제목에서 검색어 이후 부분을 추출하여 제안
                    int index = title.toLowerCase().indexOf(query.toLowerCase());
                    if (index >= 0 && index + query.length() < title.length()) {
                        return query + title.substring(index + query.length()).split("\\s+")[0];
                    }
                    return title.substring(0, Math.min(20, title.length()));
                })
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<String> getTrendingKeywords(int limit) {
        Pageable pageable = Pageable.ofSize(limit * 2); // 더 많은 결과에서 필터링
        List<String> keywords = newsRepository.findPopularKeywords(pageable);
        
        return keywords.stream()
                .filter(keyword -> keyword != null && keyword.length() > 2)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Object> getSearchStats(String query) {
        Map<String, Object> stats = new HashMap<>();
        
        // 전체 검색 결과 수
        long totalCount = newsRepository.searchByKeyword(query, Pageable.unpaged()).getTotalElements();
        stats.put("totalCount", totalCount);
        
        // 언론사별 통계
        List<Object[]> pressStats = newsRepository.getPressStatsByQuery(query);
        Map<String, Long> pressCounts = new HashMap<>();
        for (Object[] stat : pressStats) {
            pressCounts.put((String) stat[0], (Long) stat[1]);
        }
        stats.put("pressStats", pressCounts);
        
        // 카테고리별 통계
        List<Object[]> categoryStats = newsRepository.getCategoryStatsByQuery(query);
        Map<String, Long> categoryCounts = new HashMap<>();
        for (Object[] stat : categoryStats) {
            categoryCounts.put(((News.Category) stat[0]).name(), (Long) stat[1]);
        }
        stats.put("categoryStats", categoryCounts);
        
        return stats;
    }
    
    @Override
    public List<String> getRelatedKeywords(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }
        
        Pageable pageable = Pageable.ofSize(limit * 2);
        List<String> relatedTitles = newsRepository.findRelatedKeywords(query, pageable);
        
        // 제목에서 키워드 추출
        return relatedTitles.stream()
                .filter(title -> title != null && !title.isEmpty())
                .map(title -> {
                    // 제목에서 첫 번째 명사/키워드 추출
                    String[] words = title.split("\\s+");
                    return words.length > 0 ? words[0] : title.substring(0, Math.min(10, title.length()));
                })
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    @Override
    public Page<NewsListResponse> searchNewsWithHighlight(String query, Pageable pageable) {
        Page<News> newsPage = newsRepository.searchByKeyword(query, pageable);
        
        return newsPage.map(news -> {
            NewsListResponse response = convertToNewsListResponse(news);
            
            // 검색어 하이라이팅 적용
            if (query != null && !query.trim().isEmpty()) {
                String highlightedTitle = highlightKeyword(response.getTitle(), query);
                String highlightedSummary = highlightKeyword(response.getSummary(), query);
                
                response.setTitle(highlightedTitle);
                response.setSummary(highlightedSummary);
            }
            
            return response;
        });
    }
    
    @Override
    public Page<NewsListResponse> advancedSearch(String query, String category, String press, String reporter, 
                                                LocalDateTime startDate, LocalDateTime endDate, Boolean trusted, 
                                                String sortBy, Pageable pageable) {
        News.Category categoryEnum = null;
        if (category != null && !category.isEmpty()) {
            try {
                categoryEnum = News.Category.valueOf(category.toUpperCase());
            } catch (IllegalArgumentException e) {
                // 잘못된 카테고리명은 무시
            }
        }
        
        Page<News> newsPage = newsRepository.advancedSearch(query, categoryEnum, press, reporter, 
                                                           startDate, endDate, trusted, pageable);
        
        return newsPage.map(this::convertToNewsListResponse);
    }
    
    // 검색어 하이라이팅 헬퍼 메서드
    private String highlightKeyword(String text, String keyword) {
        if (text == null || keyword == null || keyword.trim().isEmpty()) {
            return text;
        }
        
        String lowerText = text.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();
        
        if (lowerText.contains(lowerKeyword)) {
            int index = lowerText.indexOf(lowerKeyword);
            String before = text.substring(0, index);
            String match = text.substring(index, index + keyword.length());
            String after = text.substring(index + keyword.length());
            
            return before + "<mark>" + match + "</mark>" + after;
        }
        
        return text;
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
        return List.of(News.Category.values())
                .stream()
                .map(this::convertToCategoryDto)
                .collect(Collectors.toList());
    }
    
    // 새로 추가된 메서드들의 구현
    @Override
    public Page<NewsListResponse> getNewsByPress(String press, Pageable pageable) {
        return newsRepository.findByPress(press, pageable)
                .map(this::convertToNewsListResponse);
    }
    
    @Override
    public List<NewsListResponse> getNewsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return newsRepository.findByCreatedAtBetween(startDate, endDate)
                .stream()
                .map(this::convertToNewsListResponse)
                .collect(Collectors.toList());
    }
    
    @Override
    public Long getNewsCount() {
        return newsRepository.count();
    }
    
    @Override
    public Long getNewsCountByCategory(News.Category category) {
        return newsRepository.countByCategory(category);
    }
    
    @Override
    public void promoteToNews(Long newsCrawlId) {
        // 크롤링된 뉴스를 승격하여 노출용 뉴스로 전환
        NewsCrawl newsCrawl = newsCrawlRepository.findById(newsCrawlId)
                .orElseThrow(() -> new NewsNotFoundException("NewsCrawl not found with id: " + newsCrawlId));
        
        // 이미 승격된 뉴스인지 확인
        List<News> existingNews = newsRepository.findByOriginalNewsId(newsCrawl.getRawId());
        if (!existingNews.isEmpty()) {
            throw new RuntimeException("이미 승격된 뉴스입니다: " + newsCrawlId);
        }
        
        // News 엔티티 생성 및 저장
        News news = News.builder()
                .originalNewsId(newsCrawl.getRawId())
                .title(newsCrawl.getTitle())
                .content(newsCrawl.getContent())
                .press(newsCrawl.getPress())
                .reporter(newsCrawl.getReporterName())
                .publishedAt(newsCrawl.getPublishedAt().toString())
                .summary(generateSummary(newsCrawl.getContent())) // 요약 생성
                .trusted(calculateTrusted(newsCrawl)) // 신뢰도 계산
                .categoryName(newsCrawl.getCategory()) // 카테고리 설정
                .dedupState(News.DedupState.KEPT) // 기본값
                .build();
        
        newsRepository.save(news);
    }
    
    @Override
    public Page<NewsCrawl> getCrawledNews(Pageable pageable) {
        return newsCrawlRepository.findAll(pageable);
    }

    // DTO 변환 메서드들
    private NewsResponse convertToNewsResponse(News news) {
        return NewsResponse.builder()
                .newsId(news.getNewsId())
                .originalNewsId(news.getOriginalNewsId())
                .title(news.getTitle())
                .content(news.getContent())
                .press(news.getPress())
                .link(null) // TODO: link 필드 추가 필요
                .summary(news.getSummary())
                .trusted(news.getTrusted() ? 1 : 0)
                .publishedAt(parsePublishedAt(news.getPublishedAt()))
                .createdAt(news.getCreatedAt())
                .reporterName(news.getReporter())
                .categoryName(news.getCategoryName().name())
                .categoryDescription(news.getCategoryName().getDescription())
                .dedupState(news.getDedupState().name())
                .dedupStateDescription(news.getDedupState().getDescription())
                .imageUrl(news.getImageUrl())
                .oidAid(news.getOidAid())
                .updatedAt(news.getUpdatedAt())
                .build();
    }
    
    private NewsListResponse convertToNewsListResponse(News news) {
        return NewsListResponse.builder()
                .newsId(news.getNewsId())
                .originalNewsId(news.getOriginalNewsId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .press(news.getPress())
                .link(null) // TODO: link 필드 추가 필요
                .trusted(news.getTrusted() ? 1 : 0)
                .publishedAt(parsePublishedAt(news.getPublishedAt()))
                .createdAt(news.getCreatedAt())
                .reporterName(news.getReporter())
                .viewCount(0) // TODO: view count 필드 추가 필요
                .categoryName(news.getCategoryName().name())
                .categoryDescription(news.getCategoryName().getDescription())
                .dedupState(news.getDedupState().name())
                .dedupStateDescription(news.getDedupState().getDescription())
                .imageUrl(news.getImageUrl())
                .oidAid(news.getOidAid())
                .updatedAt(news.getUpdatedAt())
                .build();
    }
    
    private CategoryDto convertToCategoryDto(News.Category category) {
        return CategoryDto.builder()
                .categoryCode(category.name())
                .categoryName(category.getDescription())
                .icon("📰") // 기본 아이콘
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
    private Boolean calculateTrusted(NewsCrawl newsCrawl) {
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
        
        return trusted >= 70; // 70 이상이면 true
    }

    // 안전한 날짜 파싱 메서드
    private LocalDateTime parsePublishedAt(String publishedAt) {
        if (publishedAt == null || publishedAt.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        
        try {
            // MySQL의 DATETIME 형식 (2025-08-07 11:50:01.000000) 처리
            if (publishedAt.contains(".")) {
                // 마이크로초 부분 제거
                String withoutMicroseconds = publishedAt.substring(0, publishedAt.lastIndexOf("."));
                return LocalDateTime.parse(withoutMicroseconds, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                // 일반적인 형식
                return LocalDateTime.parse(publishedAt, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception e) {
            System.err.println("날짜 파싱 실패: " + publishedAt + ", 에러: " + e.getMessage());
            return LocalDateTime.now();
        }
    }
} 