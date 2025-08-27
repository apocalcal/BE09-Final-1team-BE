package com.newnormallist.newsservice.news.service;

import com.newnormallist.newsservice.news.dto.*;
import com.newnormallist.newsservice.news.entity.*;
import com.newnormallist.newsservice.news.exception.ForbiddenAccessException;
import com.newnormallist.newsservice.news.exception.NewsHiddenException;
import com.newnormallist.newsservice.news.exception.NewsNotFoundException;

import com.newnormallist.newsservice.news.exception.ResourceNotFoundException;
import com.newnormallist.newsservice.news.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class NewsServiceImpl implements NewsService {

    @Autowired
    private NewsCrawlRepository newsCrawlRepository;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private KeywordSubscriptionRepository keywordSubscriptionRepository;

    @Autowired
    private NewsComplaintRepository newsComplaintRepository;

    @Autowired
    private NewsScrapRepository newsScrapRepository;

    @Autowired
    private ScrapStorageRepository scrapStorageRepository;


    // 크롤링 관련 메서드들
    @Override
    public NewsCrawl saveCrawledNews(NewsCrawlDto dto) {
        // 중복 체크
        if (newsCrawlRepository.existsByLinkId(dto.getLinkId())) {
            throw new RuntimeException("이미 존재하는 뉴스입니다: " + dto.getLinkId());
        }

        // Category enum 사용
        Category category = dto.getCategory();

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
            return newsRepository.findByCategory(category, pageable)
                    .map(this::convertToNewsResponse);
        } else {
            // 전체 뉴스 (최신순 정렬)
            return newsRepository.findAllByOrderByPublishedAtDesc(pageable)
                    .map(this::convertToNewsResponse);
        }
    }

    @Override
    public NewsResponse getNewsById(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsNotFoundException("존재하지 않는 뉴스입니다: " + newsId));

        // feature/changjun브랜치
        if (news.getStatus() == NewsStatus.HIDDEN) {
            throw new NewsHiddenException("신고가 누적되어 비공개 처리된 기사입니다.");
        }

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
    public Page<NewsListResponse> getNewsByCategory(Category category, Pageable pageable) {
        return newsRepository.findByCategory(category, pageable)
                .map(this::convertToNewsListResponse);
    }

    @Override
    public Page<NewsListResponse> searchNews(String query, Pageable pageable) {
        return newsRepository.searchByKeyword(query, pageable)
                .map(this::convertToNewsListResponse);
    }

    @Override
    public Page<NewsListResponse> searchNewsWithFilters(String query, String sortBy, String sortOrder,
            String category, String press, String startDate,
            String endDate, Pageable pageable) {
        // 기본 검색 결과 가져오기
        Page<News> newsPage = newsRepository.searchByKeyword(query, pageable);

        // 필터링 적용
        List<News> filteredNews = newsPage.getContent().stream()
                .filter(news -> {
                    // 카테고리 필터
                    if (category != null && !category.isEmpty()) {
                        try {
                            Category categoryEnum = Category.valueOf(category.toUpperCase());
                            if (!news.getCategoryName().equals(categoryEnum)) {
                                return false;
                            }
                        } catch (IllegalArgumentException e) {
                            return false;
                        }
                    }

                    // 언론사 필터
                    if (press != null && !press.isEmpty()) {
                        if (!news.getPress().toLowerCase().contains(press.toLowerCase())) {
                            return false;
                        }
                    }

                    // 날짜 필터
                    if (startDate != null && !startDate.isEmpty()) {
                        LocalDateTime start = parsePublishedAt(startDate);
                        if (news.getCreatedAt().isBefore(start)) {
                            return false;
                        }
                    }

                    if (endDate != null && !endDate.isEmpty()) {
                        LocalDateTime end = parsePublishedAt(endDate);
                        if (news.getCreatedAt().isAfter(end)) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

        // 정렬 적용
        if (sortBy != null && !sortBy.isEmpty()) {
            String order = (sortOrder != null && sortOrder.equalsIgnoreCase("desc")) ? "desc" : "asc";

            switch (sortBy.toLowerCase()) {
                case "date":
                case "publishedat":
                    if (order.equals("desc")) {
                        filteredNews.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
                    } else {
                        filteredNews.sort((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()));
                    }
                    break;
//                case "viewcount":
//                    if (order.equals("desc")) {
//                        filteredNews.sort((a, b) -> Integer.compare(b.getViewCount() != null ? b.getViewCount() : 0,
//                                                                   a.getViewCount() != null ? a.getViewCount() : 0));
//                    } else {
//                        filteredNews.sort((a, b) -> Integer.compare(a.getViewCount() != null ? a.getViewCount() : 0,
//                                                                   b.getViewCount() != null ? b.getViewCount() : 0));
//                    }
//                    break;
                case "title":
                    if (order.equals("desc")) {
                        filteredNews.sort((a, b) -> b.getTitle().compareTo(a.getTitle()));
                    } else {
                        filteredNews.sort((a, b) -> a.getTitle().compareTo(b.getTitle()));
                    }
                    break;
                case "press":
                    if (order.equals("desc")) {
                        filteredNews.sort((a, b) -> b.getPress().compareTo(a.getPress()));
                    } else {
                        filteredNews.sort((a, b) -> a.getPress().compareTo(b.getPress()));
                    }
                    break;
                default:
                    // 기본 정렬: 최신순
                    filteredNews.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
            }
        }

        // 페이징 적용
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();
        int start = pageNumber * pageSize;
        int end = Math.min(start + pageSize, filteredNews.size());

        List<News> pagedNews = filteredNews.subList(start, end);
        List<NewsListResponse> responseList = pagedNews.stream() // pagedNews -> filteredNews로 변경하는 것이 좋아보입니다.
                .map(this::convertToNewsListResponse)
                .collect(Collectors.toList());

        // Page 객체 생성
        return new org.springframework.data.domain.PageImpl<>(
                responseList, pageable, filteredNews.size());
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
        return List.of(Category.values())
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
        // feature/changjun브랜치
        return newsRepository.findByCreatedAtBetween(startDate, endDate)
                .stream()
                .map(this::convertToNewsListResponse) // findByPublishedAtBetween -> findByCreatedAtBetween
                .collect(Collectors.toList());
    }

    @Override
    public Long getNewsCount() {
        return newsRepository.count();
    }

    @Override
    public Long getNewsCountByCategory(Category category) {
        return newsRepository.countByCategory(category);
    }

    @Override
    public void promoteToNews(Long newsCrawlId) {
        // 크롤링된 뉴스를 승격하여 노출용 뉴스로 전환
        NewsCrawl newsCrawl = newsCrawlRepository.findById(newsCrawlId)
                .orElseThrow(() -> new NewsNotFoundException("NewsCrawl not found with id: " + newsCrawlId));

        // 이미 승격된 뉴스인지 확인
//        List<News> existingNews = newsRepository.findByOriginalNewsId(newsCrawl.getRawId());
//        if (!existingNews.isEmpty()) {
//            throw new RuntimeException("이미 승격된 뉴스입니다: " + newsCrawlId);
//        }
//
        // News 엔티티 생성 및 저장
        News news = News.builder()
                .title(newsCrawl.getTitle())
                .content(newsCrawl.getContent())
                .press(newsCrawl.getPress())
                .reporter(newsCrawl.getReporterName())
                .publishedAt(newsCrawl.getPublishedAt().toString())
                .trusted(calculateTrusted(newsCrawl)) // 신뢰도 계산
                .categoryName(newsCrawl.getCategory()) // 카테고리 설정
                .dedupState(DedupState.KEPT) // 기본값
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
                .title(news.getTitle())
                .content(news.getContent())
                .press(news.getPress())
                .link(null) // TODO: link 필드 추가 필요
                .trusted(news.getTrusted() ? 1 : 0)
                .publishedAt(parsePublishedAt(news.getPublishedAt()))
                .createdAt(news.getCreatedAt())
                .reporterName(news.getReporter())
                .categoryName(news.getCategoryName().name())
                .dedupState(news.getDedupState().name())
                .dedupStateDescription(news.getDedupState().getDescription())
                .imageUrl(news.getImageUrl())
                .oidAid(news.getOidAid())
                .updatedAt(news.getUpdatedAt()) // feature/changjun브랜치
                .build();
    }

    private NewsListResponse convertToNewsListResponse(News news) {
        return NewsListResponse.builder()
                .newsId(news.getNewsId())
                .title(news.getTitle())
                .content(news.getContent())
                .press(news.getPress())
                .link(null) // TODO: link 필드 추가 필요
                .trusted(news.getTrusted() ? 1 : 0)
                .publishedAt(parsePublishedAt(news.getPublishedAt()))
                .createdAt(news.getCreatedAt())
                .reporterName(news.getReporter())
                .viewCount(0) // TODO: view count 필드 추가 필요
                .categoryName(news.getCategoryName().name())
                .dedupState(news.getDedupState().name())
                .dedupStateDescription(news.getDedupState().getDescription())
                .imageUrl(news.getImageUrl())
                .oidAid(news.getOidAid())
                .updatedAt(news.getUpdatedAt()) // feature/changjun브랜치
                .build();
    }

    private CategoryDto convertToCategoryDto(Category category) {
        return CategoryDto.builder()
                .categoryCode(category.name())
                .categoryName(category.getCategoryName())
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

    // 키워드 구독 관련 메서드들
    @Override
    public KeywordSubscriptionDto subscribeKeyword(Long userId, String keyword) {
        // 이미 구독 중인지 확인
        if (keywordSubscriptionRepository.existsByUserIdAndKeywordAndIsActiveTrue(userId, keyword)) {
            throw new RuntimeException("이미 구독 중인 키워드입니다: " + keyword);
        }

        KeywordSubscription subscription = KeywordSubscription.builder()
                .userId(userId)
                .keyword(keyword)
                .isActive(true)
                .build();

        KeywordSubscription saved = keywordSubscriptionRepository.save(subscription);
        return convertToKeywordSubscriptionDto(saved);
    }

    @Override
    public void unsubscribeKeyword(Long userId, String keyword) {
        KeywordSubscription subscription = keywordSubscriptionRepository
                .findByUserIdAndKeywordAndIsActiveTrue(userId, keyword)
                .orElseThrow(() -> new RuntimeException("구독하지 않은 키워드입니다: " + keyword));

        subscription.setIsActive(false);
        keywordSubscriptionRepository.save(subscription);
    }

    @Override
    public List<KeywordSubscriptionDto> getUserKeywordSubscriptions(Long userId) {
        return keywordSubscriptionRepository.findByUserIdAndIsActiveTrue(userId)
                .stream()
                .map(this::convertToKeywordSubscriptionDto)
                .collect(Collectors.toList());
    }

    // 트렌딩 키워드 관련 메서드들
    @Override
    public List<TrendingKeywordDto> getTrendingKeywords(int limit) {
        // 최근 7일간의 뉴스에서 키워드 추출 및 트렌딩 점수 계산
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        // 실제 구현에서는 뉴스 내용에서 키워드를 추출하고 트렌딩 점수를 계산해야 함
        // 여기서는 간단한 예시로 인기 키워드를 반환
        return getPopularKeywords(limit);
    }

    @Override
    public List<TrendingKeywordDto> getPopularKeywords(int limit) {
        List<Object[]> popularKeywords = keywordSubscriptionRepository.findPopularKeywords();

        return popularKeywords.stream()
                .limit(limit)
                .map(result -> TrendingKeywordDto.builder()
                        .keyword((String) result[0])
                        .count((Long) result[1])
                        .trendScore((double) result[1]) // 간단히 구독 수를 트렌딩 점수로 사용
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<TrendingKeywordDto> getTrendingKeywordsByCategory(Category category, int limit) {
        // 해당 카테고리의 최근 뉴스에서 키워드 추출
        LocalDateTime weekAgo = LocalDateTime.now().minusDays(7);

        try {
            // 해당 카테고리의 최근 뉴스 조회
            Page<News> categoryNews = newsRepository.findByCategory(category, Pageable.ofSize(100));
            List<News> recentNews = categoryNews.getContent().stream()
                    .filter(news -> {
                        try {
                            LocalDateTime publishedAt = LocalDateTime.parse(news.getPublishedAt());
                            return publishedAt.isAfter(weekAgo);
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .collect(Collectors.toList());

            // 키워드 추출 및 빈도 계산
            Map<String, Long> keywordCounts = recentNews.stream()
                    .flatMap(news -> extractKeywordsFromNews(news).stream())
                    .collect(Collectors.groupingBy(keyword -> keyword, Collectors.counting()));

            return keywordCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(limit)
                    .map(entry -> TrendingKeywordDto.builder()
                            .keyword(entry.getKey())
                            .count(entry.getValue())
                            .trendScore(entry.getValue().doubleValue())
                            .build())
                    .collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("카테고리별 트렌딩 키워드 조회 실패: category={}", category, e); // feature/changjun브랜치 (Slf4j로 변경)
            return getDefaultKeywords(limit);
        }
    }

    /**
     * 뉴스에서 키워드 추출
     */
    private List<String> extractKeywordsFromNews(News news) {
        List<String> keywords = new ArrayList<>();

        // 제목에서 키워드 추출
        if (news.getTitle() != null) {
            keywords.addAll(extractKeywordsFromText(news.getTitle()));
        }

        // 내용에서 키워드 추출 (내용이 너무 길면 앞부분만 사용)
        if (news.getContent() != null) {
            String content = news.getContent();
            if (content.length() > 500) {
                content = content.substring(0, 500);
            }
            keywords.addAll(extractKeywordsFromText(content));
        }

        return keywords;
    }

    /**
     * 텍스트에서 키워드 추출
     */
    private List<String> extractKeywordsFromText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new ArrayList<>();
        }

        // 간단한 키워드 추출 로직
        return Arrays.stream(text.split("\\s+"))
                .map(word -> word.replaceAll("[^가-힣0-9A-Za-z]", ""))
                .filter(word -> word.length() >= 2 && word.matches(".*[가-힣].*"))
                .filter(word -> !STOPWORDS.contains(word))
                .collect(Collectors.toList());
    }

    /**
     * 기본 키워드 반환
     */
    private List<TrendingKeywordDto> getDefaultKeywords(int limit) {
        List<String> defaultKeywords = Arrays.asList(
                "주요뉴스", "핫이슈", "트렌드", "분석", "전망", "동향", "소식", "업데이트"
        );

        return defaultKeywords.stream()
                .limit(limit)
                .map(keyword -> TrendingKeywordDto.builder()
                        .keyword(keyword)
                        .count(1L)
                        .trendScore(1.0)
                        .build())
                .collect(Collectors.toList());
    }

    // 너무 일반적인 단어는 제외
    private static final Set<String> STOPWORDS = Set.of(
            "속보", "영상", "단독", "인터뷰", "기자", "사진", "종합", "오늘", "내일",
            "정부", "대통령", "국회", "한국", "대한민국", "뉴스", "기사", "외신",
            "관련", "이번", "지난", "현재", "최대", "최소", "전망", "분석", "현장"
    );

    private KeywordSubscriptionDto convertToKeywordSubscriptionDto(KeywordSubscription subscription) {
        return KeywordSubscriptionDto.builder()
                .subscriptionId(subscription.getSubscriptionId())
                .userId(subscription.getUserId())
                .keyword(subscription.getKeyword())
                .isActive(subscription.getIsActive())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }

    @Override
    public void scrapNews(Long newsId, Long userId) {
        // feature/changjun브랜치
        List<ScrapStorage> storages = scrapStorageRepository.findByUserId(userId);
        ScrapStorage scrapStorage;
        if (storages.isEmpty()) {
            ScrapStorage newStorage = new ScrapStorage();
            newStorage.setUserId(userId);
            newStorage.setStorageName(userId + "'s default storage");
            newStorage.setCreatedAt(LocalDateTime.now());
            newStorage.setUpdatedAt(LocalDateTime.now());
            scrapStorage = scrapStorageRepository.save(newStorage);
        } else {
            scrapStorage = storages.get(0);
        }

        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsNotFoundException("스크랩하려는 뉴스를 찾을 수 없습니다: " + newsId));

        final Integer storageId = scrapStorage.getStorageId();
        boolean isAlreadyScrapped = newsScrapRepository.findByStorageIdAndNewsNewsId(storageId, newsId).isPresent();

        if (isAlreadyScrapped) {
            log.info("사용자 ID: {}, 뉴스 ID: {}는 이미 스크랩된 항목입니다. 중복 저장을 방지합니다.", userId, newsId);
            return;
        }

        NewsScrap newScrap = new NewsScrap();
        newScrap.setNews(news);
        newScrap.setStorageId(storageId);
        newScrap.setCreatedAt(LocalDateTime.now());
        newScrap.setUpdatedAt(LocalDateTime.now());

        newsScrapRepository.save(newScrap);
    }

    @Override
    public void reportNews(Long newsId, Long userId) {
        // feature/changjun브랜치
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsNotFoundException("신고하려는 뉴스를 찾을 수 없습니다: " + newsId));

        if (news.getStatus() == NewsStatus.HIDDEN) {
            return;
        }

        NewsComplaint complaint = new NewsComplaint();
        complaint.setNewsId(newsId);
        complaint.setUserId(userId);
        complaint.setCreatedAt(LocalDateTime.now());
        newsComplaintRepository.save(complaint);

        long complaintCount = newsComplaintRepository.countByNewsId(newsId);
        if (complaintCount >= 20) {
            news.setStatus(NewsStatus.HIDDEN);
            newsRepository.save(news);
        }
    }

    // feature/changjun브랜치
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void republishNewsWithNoReports() {
        log.info("자동 뉴스 상태 복구 작업을 시작합니다...");
        List<News> hiddenNews = newsRepository.findByStatus(NewsStatus.HIDDEN);

        hiddenNews.stream()
                .filter(news -> newsComplaintRepository.countByNewsId(news.getNewsId()) == 0)
                .forEach(news -> {
                    news.setStatus(NewsStatus.PUBLISHED);
                    log.info("뉴스 ID {}의 신고 내역이 없어 상태를 PUBLISHED로 변경합니다.", news.getNewsId());
                });
        log.info("자동 뉴스 상태 복구 작업 완료. 총 {}개의 뉴스가 복구되었습니다.", hiddenNews.stream().filter(n -> n.getStatus() == NewsStatus.PUBLISHED).count());
    }

    @Override
    public List<ScrapStorageResponse> getUserScrapStorages(Long userId) {
        return scrapStorageRepository.findByUserId(userId).stream()
                .map(this::convertToScrapStorageResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ScrapStorageResponse createCollection(Long userId, String storageName) {
        ScrapStorage newStorage = ScrapStorage.builder()
                .userId(userId)
                .storageName(storageName)
                .build();
        ScrapStorage savedStorage = scrapStorageRepository.save(newStorage);
        log.info("새 컬렉션이 생성되었습니다. userId: {}, storageName: {}", userId, storageName);
        return convertToScrapStorageResponse(savedStorage);
    }

    @Override
    public void addNewsToCollection(Long userId, Integer collectionId, Long newsId) {
        // 1. 컬렉션(ScrapStorage) 조회
        ScrapStorage storage = scrapStorageRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));

        // 2. (보안) 요청한 사용자가 해당 컬렉션의 소유자인지 확인
        if (!storage.getUserId().equals(userId)) {
            throw new ForbiddenAccessException("You do not have permission to add news to this collection.");
        }

        // 3. 추가할 뉴스 조회
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsNotFoundException("News not found with id: " + newsId));

        // 4. 이미 해당 컬렉션에 스크랩된 뉴스인지 확인
        boolean isAlreadyScrapped = newsScrapRepository.findByStorageIdAndNewsNewsId(collectionId, newsId).isPresent();
        if (isAlreadyScrapped) {
            log.info("News (id: {}) is already in collection (id: {}).", newsId, collectionId);
            return; // 중복 추가 방지
        }

        // 5. 새로운 스크랩 정보 생성 및 저장
        NewsScrap newScrap = new NewsScrap();
        newScrap.setNews(news);
        newScrap.setStorageId(collectionId);

        newsScrapRepository.save(newScrap);
        log.info("Successfully added news (id: {}) to collection (id: {}).", newsId, collectionId);
    }

    private ScrapStorageResponse convertToScrapStorageResponse(ScrapStorage storage) {
        // 1. 이 컬렉션의 뉴스 개수를 데이터베이스에서 직접 셉니다.
        long count = newsScrapRepository.countByStorageId(storage.getStorageId());

        // 2. DTO를 만들 때, 방금 계산한 개수(count)를 newsCount 필드에 담아줍니다.
        return ScrapStorageResponse.builder()
                .storageId(storage.getStorageId())
                .storageName(storage.getStorageName())
                .newsCount(count) // <--- 바로 이 한 줄을 추가하는 것이 핵심입니다!
                .createdAt(storage.getCreatedAt())
                .updatedAt(storage.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ScrappedNewsResponse> getNewsInCollection(Long userId, Integer collectionId, Pageable pageable) {
        // 1. 컬렉션(ScrapStorage)이 존재하는지 확인
        ScrapStorage storage = scrapStorageRepository.findById(collectionId)
                .orElseThrow(() -> new ResourceNotFoundException("Collection not found with id: " + collectionId));

        // 2. (보안) 요청한 사용자가 해당 컬렉션의 소유자인지 확인
        if (!storage.getUserId().equals(userId)) {
            throw new ForbiddenAccessException("You do not have permission to view this collection.");
        }

        // 3. 해당 컬렉션에 속한 뉴스 스크랩 목록을 페이징하여 가져오기
        Page<NewsScrap> scraps = newsScrapRepository.findByStorageId(collectionId, pageable);

        // 4. 가져온 데이터를 API 응답용 DTO 페이지로 변환하여 반환
        return scraps.map(ScrappedNewsResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<ScrappedNewsResponse> getScrappedNews(Long userId, Pageable pageable) {
        // 1. 사용자의 모든 스크랩 보관함(ScrapStorage) 조회
        List<ScrapStorage> storages = scrapStorageRepository.findByUserId(userId);
        if (storages.isEmpty()) {
            return Page.empty(pageable);
        }
        List<Integer> storageIds = storages.stream()
                .map(ScrapStorage::getStorageId)
                .collect(Collectors.toList());

        // 2. 해당 보관함들에 속한 모든 뉴스 스크랩 목록을 페이징하여 조회
        Page<NewsScrap> scraps = newsScrapRepository.findByStorageIdIn(storageIds, pageable);

        // 3. API 응답용 DTO 페이지로 변환하여 반환
        return scraps.map(ScrappedNewsResponse::from);
    }
}
