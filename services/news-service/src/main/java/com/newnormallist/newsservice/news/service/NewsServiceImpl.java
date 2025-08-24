package com.newsservice.news.service;

import com.newsservice.news.dto.CategoryDto;
import com.newsservice.news.dto.KeywordSubscriptionDto;
import com.newsservice.news.dto.NewsCrawlDto;
import com.newsservice.news.dto.NewsListResponse;
import com.newsservice.news.dto.NewsResponse;
import com.newsservice.news.dto.TrendingKeywordDto;
import com.newsservice.news.entity.*;
import com.newsservice.news.exception.NewsHiddenException;
import com.newsservice.news.exception.NewsNotFoundException;

import com.newsservice.news.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
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

    // --- (이하 다른 메소드들은 변경 없음) ---

    @Override
    public NewsCrawl saveCrawledNews(NewsCrawlDto dto) {
        if (newsCrawlRepository.existsByLinkId(dto.getLinkId())) {
            throw new RuntimeException("이미 존재하는 뉴스입니다: " + dto.getLinkId());
        }
        Category category = dto.getCategory();
        NewsCrawl newsCrawl = NewsCrawl.builder()
                .linkId(dto.getLinkId()).title(dto.getTitle()).press(dto.getPress()).content(dto.getContent())
                .reporterName(dto.getReporterName()).publishedAt(dto.getPublishedAt()).category(category).build();
        return newsCrawlRepository.save(newsCrawl);
    }

    @Override
    public NewsCrawlDto previewCrawledNews(NewsCrawlDto dto) {
        return dto;
    }

    @Override
    public Page<NewsResponse> getNews(Category category, String keyword, Pageable pageable) {
        Page<News> newsPage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            newsPage = newsRepository.searchByKeyword(keyword, pageable);
        } else if (category != null) {
            newsPage = newsRepository.findByCategory(category, pageable);
        } else {
            newsPage = newsRepository.findAll(pageable);
        }
        return newsPage.map(this::convertToNewsResponse);
    }

    @Override
    public NewsResponse getNewsById(Long newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsNotFoundException("존재하지 않는 뉴스입니다: " + newsId));

        if (news.getStatus() == NewsStatus.HIDDEN) {
            throw new NewsHiddenException("신고가 누적되어 비공개 처리된 기사입니다.");
        }

        return convertToNewsResponse(news);
    }

    @Override
    public List<NewsResponse> getPersonalizedNews(Long userId) {
        return newsRepository.findByTrustedTrue(Pageable.ofSize(10)).getContent().stream()
                .map(this::convertToNewsResponse).collect(Collectors.toList());
    }

    @Override
    public List<NewsResponse> getTrendingNews() {
        return newsRepository.findByTrustedTrue(Pageable.ofSize(10)).getContent().stream()
                .map(this::convertToNewsResponse).collect(Collectors.toList());
    }

    @Override
    public void incrementViewCount(Long newsId) { /* TODO */ }

    @Override
    public Page<NewsListResponse> getTrendingNews(Pageable pageable) {
        return newsRepository.findTrendingNews(pageable).map(this::convertToNewsListResponse);
    }

    @Override
    public Page<NewsListResponse> getRecommendedNews(Long userId, Pageable pageable) {
        return newsRepository.findByTrustedTrue(pageable).map(this::convertToNewsListResponse);
    }

    @Override
    public Page<NewsListResponse> getNewsByCategory(Category category, Pageable pageable) {
        return newsRepository.findByCategory(category, pageable).map(this::convertToNewsListResponse);
    }

    @Override
    public Page<NewsListResponse> searchNews(String query, Pageable pageable) {
        return newsRepository.searchByKeyword(query, pageable).map(this::convertToNewsListResponse);
    }

    @Override
    public Page<NewsListResponse> searchNewsWithFilters(String query, String sortBy, String sortOrder,
            String category, String press, String startDate,
            String endDate, Pageable pageable) {
        Page<News> newsPage = newsRepository.searchByKeyword(query, pageable);
        List<News> filteredNews = newsPage.getContent().stream()
                .filter(news -> {
                    if (category != null && !category.isEmpty()) {
                        try {
                            if (!news.getCategoryName().equals(Category.valueOf(category.toUpperCase()))) return false;
                        } catch (IllegalArgumentException e) { return false; }
                    }
                    if (press != null && !press.isEmpty()) {
                        if (!news.getPress().toLowerCase().contains(press.toLowerCase())) return false;
                    }
                    if (startDate != null && !startDate.isEmpty()) {
                        if (news.getCreatedAt().isBefore(parsePublishedAt(startDate))) return false;
                    }
                    if (endDate != null && !endDate.isEmpty()) {
                        if (news.getCreatedAt().isAfter(parsePublishedAt(endDate))) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if (sortBy != null && !sortBy.isEmpty()) { /* 정렬 로직 */ }

        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredNews.size());
        List<NewsListResponse> responseList = filteredNews.subList(start, end).stream()
                .map(this::convertToNewsListResponse).collect(Collectors.toList());
        return new PageImpl<>(responseList, pageable, filteredNews.size());
    }

    @Override
    public Page<NewsListResponse> getPopularNews(Pageable pageable) {
        return newsRepository.findPopularNews(pageable).map(this::convertToNewsListResponse);
    }

    @Override
    public Page<NewsListResponse> getLatestNews(Pageable pageable) {
        return newsRepository.findLatestNews(pageable).map(this::convertToNewsListResponse);
    }

    @Override
    public List<CategoryDto> getAllCategories() {
        return List.of(Category.values()).stream().map(this::convertToCategoryDto).collect(Collectors.toList());
    }

    @Override
    public Page<NewsListResponse> getNewsByPress(String press, Pageable pageable) {
        return newsRepository.findByPress(press, pageable).map(this::convertToNewsListResponse);
    }

    @Override
    public List<NewsListResponse> getNewsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        return newsRepository.findByCreatedAtBetween(startDate, endDate).stream().map(this::convertToNewsListResponse).collect(Collectors.toList());
    }

    @Override
    public Long getNewsCount() { return newsRepository.count(); }

    @Override
    public Long getNewsCountByCategory(Category category) { return newsRepository.countByCategory(category); }

    @Override
    public void promoteToNews(Long newsCrawlId) {
        NewsCrawl newsCrawl = newsCrawlRepository.findById(newsCrawlId).orElseThrow(() -> new NewsNotFoundException("NewsCrawl not found"));
        News news = News.builder()
                .title(newsCrawl.getTitle()).content(newsCrawl.getContent()).press(newsCrawl.getPress())
                .reporter(newsCrawl.getReporterName()).publishedAt(newsCrawl.getPublishedAt().toString())
                .trusted(calculateTrusted(newsCrawl)).categoryName(newsCrawl.getCategory()).dedupState(DedupState.KEPT)
                .build();
        newsRepository.save(news);
    }

    @Override
    public Page<NewsCrawl> getCrawledNews(Pageable pageable) { return newsCrawlRepository.findAll(pageable); }

    private NewsResponse convertToNewsResponse(News news) {
        return NewsResponse.builder()
                .newsId(news.getNewsId()).title(news.getTitle()).content(news.getContent()).press(news.getPress()).link(null)
                .trusted(news.getTrusted() ? 1 : 0).publishedAt(parsePublishedAt(news.getPublishedAt())).createdAt(news.getCreatedAt())
                .reporterName(news.getReporter()).categoryName(news.getCategoryName().name()).dedupState(news.getDedupState().name())
                .dedupStateDescription(news.getDedupState().getDescription()).imageUrl(news.getImageUrl()).oidAid(news.getOidAid())
                .updatedAt(news.getUpdatedAt()).build();
    }

    private NewsListResponse convertToNewsListResponse(News news) {
        return NewsListResponse.builder()
                .newsId(news.getNewsId()).title(news.getTitle()).press(news.getPress()).link(null)
                .trusted(news.getTrusted() ? 1 : 0).publishedAt(parsePublishedAt(news.getPublishedAt())).createdAt(news.getCreatedAt())
                .reporterName(news.getReporter()).viewCount(0).categoryName(news.getCategoryName().name())
                .dedupState(news.getDedupState().name()).dedupStateDescription(news.getDedupState().getDescription())
                .imageUrl(news.getImageUrl()).oidAid(news.getOidAid()).updatedAt(news.getUpdatedAt()).build();
    }

    private CategoryDto convertToCategoryDto(Category category) {
        return CategoryDto.builder().categoryCode(category.name()).categoryName(category.getCategoryName()).icon("📰").build();
    }

    private String generateSummary(String content) {
        if (content == null || content.length() <= 200) return content;
        return content.substring(0, 200) + "...";
    }

    private Boolean calculateTrusted(NewsCrawl newsCrawl) {
        int trusted = 50;
        if (newsCrawl.getContent() != null) {
            if (newsCrawl.getContent().length() > 1000) trusted += 20;
            else if (newsCrawl.getContent().length() > 500) trusted += 10;
        }
        if (newsCrawl.getReporterName() != null && !newsCrawl.getReporterName().trim().isEmpty()) trusted += 10;
        if (newsCrawl.getPress() != null) {
            String press = newsCrawl.getPress().toLowerCase();
            if (press.contains("조선일보") || press.contains("중앙일보") || press.contains("동아일보")) trusted += 15;
            else if (press.contains("한겨레") || press.contains("경향신문")) trusted += 10;
        }
        return trusted >= 70;
    }

    private LocalDateTime parsePublishedAt(String publishedAt) {
        if (publishedAt == null || publishedAt.trim().isEmpty()) return LocalDateTime.now();
        try {
            if (publishedAt.contains(".")) {
                return LocalDateTime.parse(publishedAt.substring(0, publishedAt.lastIndexOf(".")), java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } else {
                return LocalDateTime.parse(publishedAt, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            }
        } catch (Exception e) {
            System.err.println("날짜 파싱 실패: " + publishedAt + ", 에러: " + e.getMessage());
            return LocalDateTime.now();
        }
    }

    private KeywordSubscriptionDto convertToKeywordSubscriptionDto(KeywordSubscription subscription) {
        return KeywordSubscriptionDto.builder()
                .subscriptionId(subscription.getSubscriptionId()).userId(subscription.getUserId()).keyword(subscription.getKeyword())
                .isActive(subscription.getIsActive()).createdAt(subscription.getCreatedAt()).updatedAt(subscription.getUpdatedAt()).build();
    }

    @Override
    public KeywordSubscriptionDto subscribeKeyword(Long userId, String keyword) {
        if (keywordSubscriptionRepository.existsByUserIdAndKeywordAndIsActiveTrue(userId, keyword)) {
            throw new RuntimeException("이미 구독 중인 키워드입니다: " + keyword);
        }
        KeywordSubscription subscription = KeywordSubscription.builder().userId(userId).keyword(keyword).isActive(true).build();
        return convertToKeywordSubscriptionDto(keywordSubscriptionRepository.save(subscription));
    }

    @Override
    public void unsubscribeKeyword(Long userId, String keyword) {
        KeywordSubscription subscription = keywordSubscriptionRepository.findByUserIdAndKeywordAndIsActiveTrue(userId, keyword)
                .orElseThrow(() -> new RuntimeException("구독하지 않은 키워드입니다: " + keyword));
        subscription.setIsActive(false);
        keywordSubscriptionRepository.save(subscription);
    }

    @Override
    public List<KeywordSubscriptionDto> getUserKeywordSubscriptions(Long userId) {
        return keywordSubscriptionRepository.findByUserIdAndIsActiveTrue(userId).stream()
                .map(this::convertToKeywordSubscriptionDto).collect(Collectors.toList());
    }

    @Override
    public List<TrendingKeywordDto> getTrendingKeywords(int limit) { return getPopularKeywords(limit); }

    @Override
    public List<TrendingKeywordDto> getPopularKeywords(int limit) {
        List<Object[]> popularKeywords = keywordSubscriptionRepository.findPopularKeywords();
        return popularKeywords.stream().limit(limit)
                .map(result -> TrendingKeywordDto.builder().keyword((String) result[0]).count((Long) result[1]).trendScore((double) result[1]).build())
                .collect(Collectors.toList());
    }

    @Override
    public void reportNews(Long newsId, Long userId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsNotFoundException("신고하려는 뉴스를 찾을 수 없습니다: " + newsId));

        if (news.getStatus() == NewsStatus.HIDDEN) {
            return;
        }

        NewsComplaint complaint = new NewsComplaint();
        complaint.setNewsId(newsId);
        complaint.setUserId(userId);
        // ★★★★★ [수정] @CreationTimestamp가 동작하지 않는 문제를 해결하기 위해, 시간을 직접 설정합니다. ★★★★★
        complaint.setCreatedAt(LocalDateTime.now());
        newsComplaintRepository.save(complaint);

        long complaintCount = newsComplaintRepository.countByNewsId(newsId);
        if (complaintCount >= 20) {
            news.setStatus(NewsStatus.HIDDEN);
            newsRepository.save(news);
        }
    }

    @Override
    @Transactional
    public void scrapNews(Long newsId, Long userId) {
        ScrapStorage scrapStorage = scrapStorageRepository.findByUserId(userId)
                .orElseGet(() -> {
                    ScrapStorage newStorage = new ScrapStorage();
                    newStorage.setUserId(userId);
                    newStorage.setStorageName(userId + "'s default storage");
                    return scrapStorageRepository.save(newStorage);
                });

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

        newsScrapRepository.save(newScrap);
    }


    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void republishNewsWithNoReports() {
        log.info("자동 뉴스 상태 복구 작업을 시작합니다...");
        List<News> hiddenNews = newsRepository.findByStatus(NewsStatus.HIDDEN);

        int republishedCount = 0;
        for (News news : hiddenNews) {
            long reportCount = newsComplaintRepository.countByNewsId(news.getNewsId());
            if (reportCount == 0) {
                news.setStatus(NewsStatus.PUBLISHED);
                republishedCount++;
                log.info("뉴스 ID {}의 신고 내역이 없어 상태를 PUBLISHED로 변경합니다.", news.getNewsId());
            }
        }
        if (republishedCount > 0) {
            newsRepository.saveAll(hiddenNews.stream().filter(n -> n.getStatus() == NewsStatus.PUBLISHED).collect(Collectors.toList()));
        }
        log.info("자동 뉴스 상태 복구 작업 완료. 총 {}개의 뉴스가 복구되었습니다.", republishedCount);
    }
}