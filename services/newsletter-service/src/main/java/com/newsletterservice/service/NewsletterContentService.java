package com.newsletterservice.service;

import com.newsletterservice.client.dto.NewsResponse;
import com.newsletterservice.dto.NewsletterContent;
import com.newsletterservice.dto.NewsletterCreateRequest;
import com.newsletterservice.entity.NewsCategory;
import com.newsletterservice.entity.Subscription;
import com.newsletterservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsletterContentService {
    
    private final PersonalizationService personalizationService;
    private final ContentGenerationService contentGenerationService;
    private final SubscriptionRepository subscriptionRepository;
    
    /**
     * 개인화된 뉴스레터 콘텐츠 생성
     */
    public NewsletterContent buildPersonalizedContent(Long userId, Long newsletterId) {
        log.info("Building personalized content for user: {}, newsletter: {}", userId, newsletterId);
        
        // 1. 사용자 구독 정보 확인
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUserId(userId);
        boolean isPersonalized = subscriptionOpt.map(Subscription::isPersonalized).orElse(false);
        
        // 2. 개인화된 뉴스 가져오기 (우리가 만든 PersonalizationService 활용!)
        List<NewsResponse> personalizedNews = personalizationService.getPersonalizedNews(userId, 8);
        
        // 3. 트렌딩 뉴스 가져오기 (다양성 확보)
        List<NewsResponse> trendingNews = getTrendingNews(3);
        
        // 4. 섹션 구성
        List<NewsletterContent.Section> sections = new ArrayList<>();
        
        // 개인화 섹션
        if (!personalizedNews.isEmpty()) {
            sections.add(NewsletterContent.Section.builder()
                .heading("🎯 당신을 위한 맞춤 뉴스")
                .sectionType("PERSONALIZED")
                .description("당신의 관심사와 행동 패턴을 분석한 개인화된 뉴스입니다.")
                .articles(convertToArticles(personalizedNews, userId))
                .build());
        }
        
        // 트렌딩 섹션
        if (!trendingNews.isEmpty()) {
            sections.add(NewsletterContent.Section.builder()
                .heading("🔥 지금 뜨는 뉴스")
                .sectionType("TRENDING")
                .description("현재 가장 많은 관심을 받고 있는 뉴스입니다.")
                .articles(convertToArticles(trendingNews, userId))
                .build());
        }
        
        // 5. 콘텐츠 생성
        return NewsletterContent.builder()
            .title("개인화 뉴스레터 #" + newsletterId)
            .personalized(isPersonalized)
            .userId(userId)
            .newsletterId(newsletterId)
            .sections(sections)
            .generatedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * 카테고리별 뉴스레터 콘텐츠 생성
     */
    public NewsletterContent buildCategoryContent(Long userId, Long newsletterId, List<NewsCategory> categories) {
        log.info("Building category content for user: {}, newsletter: {}, categories: {}", 
                userId, newsletterId, categories);
        
        List<NewsletterContent.Section> sections = new ArrayList<>();
        
        for (NewsCategory category : categories) {
            List<NewsResponse> categoryNews = getNewsByCategory(category, 3);
            if (!categoryNews.isEmpty()) {
                sections.add(NewsletterContent.Section.builder()
                    .heading(category.getCategoryName() + " 뉴스")
                    .sectionType("CATEGORY")
                    .description(category.getCategoryName() + " 카테고리의 최신 뉴스입니다.")
                    .articles(convertToArticles(categoryNews, userId))
                    .build());
            }
        }
        
        return NewsletterContent.builder()
            .title("카테고리 뉴스레터 #" + newsletterId)
            .personalized(false)
            .userId(userId)
            .newsletterId(newsletterId)
            .sections(sections)
            .generatedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * 최신 뉴스 기반 뉴스레터 콘텐츠 생성
     */
    public NewsletterContent buildLatestContent(Long userId, Long newsletterId) {
        log.info("Building latest content for user: {}, newsletter: {}", userId, newsletterId);
        
        List<NewsResponse> latestNews = getLatestNews(10);
        
        List<NewsletterContent.Section> sections = List.of(
            NewsletterContent.Section.builder()
                .heading("📰 최신 뉴스")
                .sectionType("LATEST")
                .description("가장 최근에 발행된 뉴스들입니다.")
                .articles(convertToArticles(latestNews, userId))
                .build()
        );
        
        return NewsletterContent.builder()
            .title("최신 뉴스레터 #" + newsletterId)
            .personalized(false)
            .userId(userId)
            .newsletterId(newsletterId)
            .sections(sections)
            .generatedAt(LocalDateTime.now())
            .build();
    }
    
    // ===== Private helper methods =====
    
    private List<NewsResponse> getTrendingNews(int limit) {
        try {
            return contentGenerationService.getPersonalizedNewsData(
                NewsletterCreateRequest.builder().newsletterId(1L).build(), 
                List.of()
            ).stream().limit(limit).toList();
        } catch (Exception e) {
            log.warn("Failed to get trending news", e);
            return List.of();
        }
    }
    
    private List<NewsResponse> getNewsByCategory(NewsCategory category, int limit) {
        try {
            return contentGenerationService.getCategoryNewsData(
                NewsletterCreateRequest.builder().newsletterId(1L).build(),
                Set.of(category)
            ).stream().limit(limit).toList();
        } catch (Exception e) {
            log.warn("Failed to get news for category: {}", category, e);
            return List.of();
        }
    }
    
    private List<NewsResponse> getLatestNews(int limit) {
        try {
            return contentGenerationService.getLatestNewsData(
                NewsletterCreateRequest.builder().newsletterId(1L).build()
            ).stream().limit(limit).toList();
        } catch (Exception e) {
            log.warn("Failed to get latest news", e);
            return List.of();
        }
    }
    
    private List<NewsletterContent.Article> convertToArticles(List<NewsResponse> newsList, Long userId) {
        return newsList.stream()
            .map(news -> convertToArticle(news, userId))
            .toList();
    }
    
    private NewsletterContent.Article convertToArticle(NewsResponse news, Long userId) {
        return NewsletterContent.Article.builder()
            .id(news.getId())
            .title(news.getTitle())
            .summary(news.getSummary())
            .category(news.getCategory())
            .url(news.getSourceUrl()) // sourceUrl 사용
            .publishedAt(news.getCreatedAt())
            .personalizedScore(personalizationService.getPersonalizationScore(news, userId))
            .imageUrl(news.getImageUrl())
            .viewCount(null) // NewsResponse에 없음
            .shareCount(null) // NewsResponse에 없음
            .build();
    }
}
