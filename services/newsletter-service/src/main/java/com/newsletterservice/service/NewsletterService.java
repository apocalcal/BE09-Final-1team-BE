package com.newsletterservice.service;

import com.newsletterservice.client.NewsServiceClient;
import com.newsletterservice.client.UserServiceClient;
import com.newsletterservice.client.dto.CategoryResponse;
import com.newsletterservice.client.dto.NewsResponse;
import com.newsletterservice.client.dto.ReadHistoryResponse;
import com.newsletterservice.client.dto.TrendingKeywordDto;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.common.exception.NewsletterException;
import com.newsletterservice.dto.*;
import com.newsletterservice.entity.*;
import com.newsletterservice.repository.NewsletterDeliveryRepository;
import com.newsletterservice.repository.SubscriptionRepository;
import com.newsletterservice.entity.NewsCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class NewsletterService {

    // ========================================
    // Repository Dependencies
    // ========================================
    private final NewsletterDeliveryRepository deliveryRepository;
    private final SubscriptionRepository subscriptionRepository;

    // ========================================
    // Client Dependencies
    // ========================================
    private final NewsServiceClient newsServiceClient;
    private final UserServiceClient userServiceClient;

    // ========================================
    // Service Dependencies
    // ========================================
    private final EmailNewsletterRenderer emailRenderer;
    private final UserReadHistoryService userReadHistoryService; // UserNewsInteraction 대신 사용

    // ========================================
    // Constants
    // ========================================
    private static final int MAX_ITEMS = 8;
    private static final int PER_CATEGORY_LIMIT = 3;
    private static final int MAX_RETRY_COUNT = 3;
    private static final double HIGH_ENGAGEMENT_THRESHOLD = 40.0;
    private static final double MEDIUM_ENGAGEMENT_THRESHOLD = 25.0;
    private static final double LOW_ENGAGEMENT_THRESHOLD = 15.0;
    private static final int MAX_CATEGORIES_PER_USER = 3; // 사용자당 최대 3개 카테고리 구독 제한

    // ========================================
    // 1. 구독 관리 기능
    // ========================================

    /**
     * 구독 생성/업데이트
     */
    public SubscriptionResponse subscribe(SubscriptionRequest request, String userId) {
        log.info("구독 생성 요청: userId={}, categories={}", userId, request.getPreferredCategories());
        
        try {
            validateSubscriptionRequest(request, userId);
            
            Long userIdLong = Long.valueOf(userId);
            Optional<Subscription> existingSubscription = subscriptionRepository.findByUserId(userIdLong);
            
            Subscription subscription = existingSubscription.orElse(new Subscription());
            updateSubscriptionFromRequest(subscription, request, userIdLong);
            
            Subscription savedSubscription = subscriptionRepository.save(subscription);
            log.info("구독 처리 완료: subscriptionId={}", savedSubscription.getId());
            
            return convertToSubscriptionResponse(savedSubscription);
            
        } catch (NewsletterException e) {
            throw e;
        } catch (Exception e) {
            log.error("구독 생성 실패: userId={}", userId, e);
            throw new NewsletterException("구독 처리 중 오류가 발생했습니다.", "SUBSCRIPTION_ERROR");
        }
    }

    /**
     * 구독 해지 (권한 확인 포함)
     */
    public void unsubscribe(Long subscriptionId, Long userId) {
        log.info("구독 해지: subscriptionId={}, userId={}", subscriptionId, userId);
        
        try {
            Subscription subscription = getSubscriptionWithPermissionCheck(subscriptionId, userId);
            
            subscription.setStatus(SubscriptionStatus.UNSUBSCRIBED);
            subscription.setUnsubscribedAt(LocalDateTime.now());
            subscription.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            
            log.info("구독 해지 완료: subscriptionId={}", subscriptionId);
            
        } catch (NewsletterException e) {
            throw e;
        } catch (Exception e) {
            log.error("구독 해지 중 오류 발생", e);
            throw new NewsletterException("구독 해지 중 오류가 발생했습니다.", "UNSUBSCRIBE_ERROR");
        }
    }

    /**
     * 구독 정보 조회
     */
    public SubscriptionResponse getSubscription(Long subscriptionId, Long userId) {
        log.info("구독 정보 조회: subscriptionId={}, userId={}", subscriptionId, userId);
        
        try {
            Subscription subscription = getSubscriptionWithPermissionCheck(subscriptionId, userId);
            return convertToSubscriptionResponse(subscription);
            
        } catch (NewsletterException e) {
            throw e;
        } catch (Exception e) {
            log.error("구독 정보 조회 중 오류 발생", e);
            throw new NewsletterException("구독 정보 조회 중 오류가 발생했습니다.", "SUBSCRIPTION_FETCH_ERROR");
        }
    }

    /**
     * 내 구독 목록 조회
     */
    public List<SubscriptionResponse> getMySubscriptions(Long userId) {
        log.info("내 구독 목록 조회: userId={}", userId);
        
        try {
            List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId)
                    .map(List::of)
                    .orElse(List.of());
            
            return subscriptions.stream()
                    .map(this::convertToSubscriptionResponse)
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("내 구독 목록 조회 중 오류 발생", e);
            throw new NewsletterException("구독 목록 조회 중 오류가 발생했습니다.", "SUBSCRIPTION_LIST_ERROR");
        }
    }

    /**
     * 활성 구독 목록 조회
     */
    public List<SubscriptionResponse> getActiveSubscriptions(Long userId) {
        log.info("활성 구독 목록 조회: userId={}", userId);
        
        try {
            List<Subscription> subscriptions = subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);
            return subscriptions.stream()
                    .map(this::convertToSubscriptionResponse)
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("활성 구독 목록 조회 중 오류 발생", e);
            throw new NewsletterException("활성 구독 목록 조회 중 오류가 발생했습니다.", "ACTIVE_SUBSCRIPTION_ERROR");
        }
    }

    /**
     * 구독 상태 변경
     */
    public SubscriptionResponse changeSubscriptionStatus(Long subscriptionId, Long userId, String newStatus) {
        log.info("구독 상태 변경: subscriptionId={}, userId={}, newStatus={}", subscriptionId, userId, newStatus);
        
        try {
            Subscription subscription = getSubscriptionWithPermissionCheck(subscriptionId, userId);
            
            SubscriptionStatus status = validateAndParseStatus(newStatus);
            subscription.setStatus(status);
            subscription.setUpdatedAt(LocalDateTime.now());
            subscription = subscriptionRepository.save(subscription);
            
            return convertToSubscriptionResponse(subscription);
            
        } catch (NewsletterException e) {
            throw e;
        } catch (Exception e) {
            log.error("구독 상태 변경 중 오류 발생", e);
            throw new NewsletterException("구독 상태 변경 중 오류가 발생했습니다.", "STATUS_CHANGE_ERROR");
        }
    }

    /**
     * 구독 상태 확인
     */
    public boolean isSubscribed(Long userId) {
        return subscriptionRepository.findByUserId(userId)
                .map(sub -> sub.getStatus() == SubscriptionStatus.ACTIVE)
                .orElse(false);
    }

    /**
     * 구독 소유권 확인
     */
    public boolean isOwnerOfSubscription(Long subscriptionId, Long userId) {
        return subscriptionRepository.findById(subscriptionId)
                .map(subscription -> subscription.getUserId().equals(userId))
                .orElse(false);
    }

    // ========================================
    // 2. 뉴스레터 콘텐츠 생성 기능
    // ========================================

    /**
     * 개인화된 뉴스레터 콘텐츠 생성
     */
    public NewsletterContent buildPersonalizedContent(Long userId, Long newsletterId) {
        log.info("개인화된 뉴스레터 콘텐츠 생성: userId={}, newsletterId={}", userId, newsletterId);
        
        if (!isSubscribed(userId)) {
            throw new NewsletterException("활성 구독이 필요합니다.", "SUBSCRIPTION_REQUIRED");
        }

        List<CategoryResponse> preferences = getUserPreferences(userId);
        boolean hasPreferences = preferences != null && !preferences.isEmpty();

        List<NewsResponse> selectedNews = collectNewsContent(preferences, hasPreferences);
        
        return NewsletterContent.builder()
                .newsletterId(newsletterId)
                .userId(userId)
                .personalized(hasPreferences)
                .title(generateTitle(hasPreferences))
                .generatedAt(LocalDateTime.now())
                .sections(buildSections(selectedNews, hasPreferences))
                .build();
    }

    /**
     * 카테고리별 맞춤 뉴스레터 콘텐츠 생성
     */
    public NewsletterContent buildCategoryContent(String category, Long newsletterId, int limit) {
        log.info("카테고리별 뉴스레터 콘텐츠 생성: category={}, limit={}", category, limit);
        
        try {
            // NewsServiceClient의 실제 API 사용
            List<NewsResponse> categoryNews = fetchNewsByCategory(category, 0, limit);
            
            return NewsletterContent.builder()
                    .newsletterId(newsletterId)
                    .userId(null)
                    .personalized(false)
                    .title(category + " 카테고리 뉴스")
                    .generatedAt(LocalDateTime.now())
                    .sections(buildCategorySections(categoryNews, category))
                    .build();
                    
        } catch (Exception e) {
            log.error("카테고리 뉴스레터 콘텐츠 생성 실패: category={}", category, e);
            throw new NewsletterException("카테고리 콘텐츠 생성 중 오류가 발생했습니다.", "CONTENT_GENERATION_ERROR");
        }
    }

    /**
     * 실시간 뉴스레터 미리보기 생성
     */
    public NewsletterPreview generateNewsletterPreview(Long userId) {
        log.info("뉴스레터 미리보기 생성: userId={}", userId);
        
        try {
            NewsletterContent content = buildPersonalizedContent(userId, null);
            String htmlPreview = emailRenderer.renderToHtml(content);

            return NewsletterPreview.builder()
                    .userId(userId)
                    .title(content.getTitle())
                    .htmlContent(htmlPreview)
                    .articleCount(content.getSections().stream()
                            .mapToInt(section -> section.getArticles().size())
                            .sum())
                    .generatedAt(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("뉴스레터 미리보기 생성 실패: userId={}", userId, e);
            throw new NewsletterException("미리보기 생성 중 오류가 발생했습니다.", "PREVIEW_ERROR");
        }
    }

    // ========================================
    // 3. 뉴스 검색 및 조회 기능
    // ========================================

    /**
     * 향상된 뉴스레터 목록 조회
     */
    public Page<EnhancedNewsletterInfo> getEnhancedNewsletters(Pageable pageable, String category, Long userId) {
        log.info("향상된 뉴스레터 목록 조회: category={}, userId={}", category, userId);
        
        try {
            List<NewsResponse> news = category != null ? 
                    fetchNewsByCategory(category, pageable.getPageNumber(), pageable.getPageSize()) : 
                    fetchLatestNews(null, pageable.getPageSize());
            
            List<EnhancedNewsletterInfo> enhancedList = news.stream()
                    .map(this::convertToEnhancedInfo)
                    .collect(Collectors.toList());
            
            return new PageImpl<>(enhancedList, pageable, enhancedList.size());
            
        } catch (Exception e) {
            log.error("향상된 뉴스레터 목록 조회 실패", e);
            throw new NewsletterException("뉴스레터 목록 조회 중 오류가 발생했습니다.", "NEWSLETTER_LIST_ERROR");
        }
    }

    /**
     * 카테고리별 실시간 뉴스 주제 조회
     */
    public List<LiveTopicResponse> getLiveTopicsForCategory(String category, int limit) {
        log.info("카테고리별 실시간 주제 조회: category={}, limit={}", category, limit);
        
        try {
            List<NewsResponse> news = fetchNewsByCategory(category, 0, limit * 2);
            Map<String, List<NewsResponse>> topicGroups = news.stream()
                    .collect(Collectors.groupingBy(n -> extractTopic(n.getTitle())));
            
            return topicGroups.entrySet().stream()
                    .limit(limit)
                    .map(entry -> LiveTopicResponse.builder()
                            .topic(entry.getKey())
                            .category(category)
                            .articleCount(entry.getValue().size())
                            .latestUpdate(entry.getValue().stream()
                                    .map(NewsResponse::getPublishedAt)
                                    .max(LocalDateTime::compareTo)
                                    .orElse(LocalDateTime.now()))
                            .build())
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("실시간 주제 조회 실패: category={}", category, e);
            throw new NewsletterException("실시간 주제 조회 중 오류가 발생했습니다.", "LIVE_TOPICS_ERROR");
        }
    }

    /**
     * 카테고리별 최신 헤드라인 조회
     */
    public List<NewsletterContent.Article> getCategoryHeadlines(String category, int limit) {
        log.info("카테고리별 헤드라인 조회: category={}, limit={}", category, limit);
        
        try {
            List<NewsResponse> news = fetchNewsByCategory(category, 0, limit);
            return news.stream()
                    .map(this::toContentArticle)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("카테고리 헤드라인 조회 실패: category={}", category, e);
            throw new NewsletterException("헤드라인 조회 중 오류가 발생했습니다.", "HEADLINE_ERROR");
        }
    }

    /**
     * 고급 뉴스 검색
     */
    public Page<NewsResponse> searchNews(NewsSearchRequest request, Pageable pageable) {
        log.info("뉴스 검색: keyword={}, category={}", request.getKeyword(), request.getCategory());
        
        try {
            // NewsServiceClient의 searchNews API 사용
            ApiResponse<List<NewsResponse>> response = newsServiceClient.searchNews(
                    request.getKeyword(), 
                    pageable.getPageNumber(), 
                    pageable.getPageSize()
            );
            
            List<NewsResponse> searchResults = response != null && response.getData() != null ? 
                    response.getData() : new ArrayList<>();
            
            // 카테고리 필터링 (필요한 경우)
            if (request.getCategory() != null && !request.getCategory().isEmpty()) {
                searchResults = searchResults.stream()
                        .filter(news -> request.getCategory().equals(news.getCategory()))
                        .collect(Collectors.toList());
            }
            
            return new PageImpl<>(searchResults, pageable, searchResults.size());
            
        } catch (Exception e) {
            log.error("뉴스 검색 실패: keyword={}", request.getKeyword(), e);
            throw new NewsletterException("뉴스 검색 중 오류가 발생했습니다.", "SEARCH_ERROR");
        }
    }

    /**
     * 실시간 뉴스 필터링
     */
    public List<NewsResponse> filterNews(NewsFilterRequest request) {
        log.info("뉴스 필터링: categories={}, startDate={}, endDate={}", request.getCategories(), request.getStartDate(), request.getEndDate());
        
        try {
            List<NewsResponse> allNews = fetchLatestNews(null, 100); // 충분한 양의 뉴스 가져오기
            
            return allNews.stream()
                    .filter(news -> matchesFilter(news, request))
                    .limit(request.getLimit() != null ? request.getLimit() : 20)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("뉴스 필터링 실패", e);
            throw new NewsletterException("뉴스 필터링 중 오류가 발생했습니다.", "FILTER_ERROR");
        }
    }

    // ========================================
    // 4. 발송 관리 기능
    // ========================================

    /**
     * 뉴스레터 즉시 발송
     */
    public DeliveryStats sendNewsletterNow(NewsletterDeliveryRequest request, Long senderId) {
        log.info("뉴스레터 즉시 발송: newsletterId={}, targetUsers={}", 
                request.getNewsletterId(), request.getTargetUserIds().size());
        
        try {
            return processDeliveryRequest(request, false);
        } catch (Exception e) {
            log.error("뉴스레터 즉시 발송 실패", e);
            throw new NewsletterException("뉴스레터 발송 중 오류가 발생했습니다.", "DELIVERY_ERROR");
        }
    }

    /**
     * 뉴스레터 예약 발송
     */
    public DeliveryStats scheduleNewsletter(NewsletterDeliveryRequest request, Long senderId) {
        log.info("뉴스레터 예약 발송: newsletterId={}, scheduledAt={}", 
                request.getNewsletterId(), request.getScheduledAt());
        
        try {
            return processDeliveryRequest(request, true);
        } catch (Exception e) {
            log.error("뉴스레터 예약 발송 실패", e);
            throw new NewsletterException("뉴스레터 예약 중 오류가 발생했습니다.", "SCHEDULE_ERROR");
        }
    }

    /**
     * 발송 취소
     */
    public void cancelDelivery(Long deliveryId, Long userId) {
        log.info("발송 취소: deliveryId={}, userId={}", deliveryId, userId);
        
        try {
            NewsletterDelivery delivery = getDeliveryWithPermissionCheck(deliveryId, userId);
            
            if (delivery.getStatus() == DeliveryStatus.SENT || delivery.getStatus() == DeliveryStatus.FAILED) {
                throw new NewsletterException("이미 처리된 발송은 취소할 수 없습니다.", "INVALID_STATUS");
            }
            
            delivery.updateStatus(DeliveryStatus.CANCELLED);
            delivery.setUpdatedAt(LocalDateTime.now());
            deliveryRepository.save(delivery);
            
        } catch (NewsletterException e) {
            throw e;
        } catch (Exception e) {
            log.error("발송 취소 실패: deliveryId={}", deliveryId, e);
            throw new NewsletterException("발송 취소 중 오류가 발생했습니다.", "CANCEL_ERROR");
        }
    }

    /**
     * 발송 재시도
     */
    public void retryDelivery(Long deliveryId, Long userId) {
        log.info("발송 재시도: deliveryId={}, userId={}", deliveryId, userId);
        
        try {
            NewsletterDelivery delivery = getDeliveryWithPermissionCheck(deliveryId, userId);
            
            if (delivery.getStatus() != DeliveryStatus.FAILED) {
                throw new NewsletterException("실패한 발송만 재시도할 수 있습니다.", "INVALID_STATUS");
            }
            
            if (delivery.getRetryCount() >= MAX_RETRY_COUNT) {
                throw new NewsletterException("최대 재시도 횟수를 초과했습니다.", "MAX_RETRY_EXCEEDED");
            }
            
            retryFailedDelivery(delivery);
            
        } catch (NewsletterException e) {
            throw e;
        } catch (Exception e) {
            log.error("발송 재시도 실패: deliveryId={}", deliveryId, e);
            throw new NewsletterException("발송 재시도 중 오류가 발생했습니다.", "RETRY_ERROR");
        }
    }

    // ========================================
    // 5. 분석 및 추천 기능
    // ========================================

    /**
     * 사용자 맞춤 뉴스 추천 (UserReadHistory 기반)
     */
    public List<NewsletterContent.Article> getPersonalizedRecommendations(Long userId, int limit) {
        log.info("사용자 맞춤 추천: userId={}, limit={}", userId, limit);
        
        try {
            List<CategoryResponse> preferences = getUserPreferences(userId);
            
            // UserReadHistoryService를 사용하여 최근 읽은 뉴스 기록 조회
            List<ReadHistoryResponse> recentReadHistory = 
                    userReadHistoryService.getRecentReadHistory(userId, 30);
            
            Map<String, Double> categoryScores = calculateCategoryScores(preferences, recentReadHistory);
            List<NewsResponse> candidateNews = fetchRecommendationCandidates(categoryScores, limit * 2);
            
            // 읽지 않은 뉴스만 필터링
            List<Long> candidateNewsIds = candidateNews.stream()
                    .map(NewsResponse::getId)
                    .collect(Collectors.toList());
            List<Long> unreadNewsIds = userReadHistoryService.filterUnreadNewsIds(userId, candidateNewsIds);
            
            // 필터링된 뉴스만 반환
            List<NewsResponse> filteredNews = candidateNews.stream()
                    .filter(news -> unreadNewsIds.contains(news.getId()))
                    .collect(Collectors.toList());
            
            return filteredNews.stream()
                    .map(news -> {
                        double score = calculatePersonalizationScore(news, categoryScores);
                        NewsletterContent.Article article = toContentArticle(news);
                        article.setPersonalizedScore(score);
                        return article;
                    })
                    .sorted((a, b) -> Double.compare(b.getPersonalizedScore(), a.getPersonalizedScore()))
                    .limit(limit)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("맞춤 추천 생성 실패: userId={}", userId, e);
            throw new NewsletterException("맞춤 추천 생성 중 오류가 발생했습니다.", "RECOMMENDATION_ERROR");
        }
    }

    /**
     * 실시간 트렌딩 뉴스 조회
     */
    public List<NewsletterContent.Article> getTrendingNewsArticles(int limit) {
        log.info("트렌딩 뉴스 조회: limit={}", limit);
        
        try {
            // NewsServiceClient의 getTrendingNews API 사용
            ApiResponse<List<NewsResponse>> response = newsServiceClient.getTrendingNews(24, limit);
            List<NewsResponse> trendingNews = response != null && response.getData() != null ? 
                    response.getData() : new ArrayList<>();
            
            return trendingNews.stream()
                    .map(news -> NewsletterContent.Article.builder()
                            .id(news.getId())
                            .title(news.getTitle())
                            .category(news.getCategory())
                            .trendScore(0.0)
                            .publishedAt(news.getPublishedAt())
                            .viewCount(0L) // TODO: 실제 조회수 연동
                            .shareCount(0L) // TODO: 실제 공유수 연동
                            .build())
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("트렌딩 뉴스 조회 실패", e);
            throw new NewsletterException("트렌딩 뉴스 조회 중 오류가 발생했습니다.", "TRENDING_ERROR");
        }
    }

    /**
     * 사용자 참여도 분석
     */
    @Transactional(readOnly = true)
    public UserEngagement analyzeUserEngagement(Long userId, int days) {
        log.info("사용자 참여도 분석: userId={}, days={}", userId, days);
        
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            
            // Repository 메서드가 없을 수 있으므로 기본 로직으로 대체
            List<NewsletterDelivery> deliveries = deliveryRepository.findByUserIdAndCreatedAtAfter(userId, since);
            
            long totalReceived = deliveries.size();
            long totalOpened = deliveries.stream().mapToLong(d -> d.getOpenedAt() != null ? 1 : 0).sum();
            
            double engagementRate = totalReceived > 0 ? (double) totalOpened / totalReceived * 100 : 0;
            String recommendation = generateEngagementRecommendation(engagementRate, null, totalReceived);
            
            return UserEngagement.builder()
                    .userId(userId)
                    .totalReceived(totalReceived)
                    .totalOpened(totalOpened)
                    .engagementRate(engagementRate)
                    .recommendation(recommendation)
                    .analysisPeriod(days)
                    .build();
                    
        } catch (Exception e) {
            log.error("참여도 분석 실패: userId={}", userId, e);
            return createEmptyEngagement(userId);
        }
    }

    /**
     * 사용자 참여도 상세 분석 (UserReadHistory 기반)
     */
    public DetailedUserEngagement analyzeDetailedUserEngagement(Long userId, int days) {
        log.info("사용자 상세 참여도 분석: userId={}, days={}", userId, days);
        
        try {
            UserEngagement basicEngagement = analyzeUserEngagement(userId, days);
            
            // UserReadHistoryService를 사용하여 최근 읽은 뉴스 기록 조회
            List<ReadHistoryResponse> recentReadHistory = userReadHistoryService.getRecentReadHistory(userId, days);
            
            // 카테고리별 읽은 뉴스 수 계산
            Map<String, Long> categoryInteractions = recentReadHistory.stream()
                    .filter(history -> history.getCategoryName() != null)
                    .collect(Collectors.groupingBy(
                            ReadHistoryResponse::getCategoryName,
                            Collectors.counting()
                    ));
            
            // 읽기 타입별 분포 계산 (모든 기록을 'READ'로 간주)
            Map<String, Long> typeDistribution = new HashMap<>();
            typeDistribution.put("READ", (long) recentReadHistory.size());
            
            // 카테고리별 평균 읽기 시간 계산 (기본값 사용)
            Map<String, Double> averageReadingTimeByCategory = categoryInteractions.entrySet().stream()
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> 3.0 // 기본 읽기 시간 3분
                    ));
            
            return DetailedUserEngagement.builder()
                    .basicEngagement(basicEngagement)
                    .categoryInteractions(categoryInteractions)
                    .interactionTypeDistribution(typeDistribution)
                    .totalInteractions(recentReadHistory.size())
                    .mostActiveHour(calculateMostActiveHourFromReadHistory(recentReadHistory))
                    .engagementTrend(calculateEngagementTrend(userId, days))
                    .averageReadingTimeByCategory(averageReadingTimeByCategory)
                    .build();
                    
        } catch (Exception e) {
            log.error("상세 참여도 분석 실패: userId={}", userId, e);
            throw new NewsletterException("상세 참여도 분석 중 오류가 발생했습니다.", "DETAILED_ENGAGEMENT_ERROR");
        }
    }

    /**
     * 카테고리별 성과 비교 분석
     */
    public CategoryPerformanceAnalysis analyzeCategoryPerformance(int days) {
        log.info("카테고리 성과 분석: days={}", days);
        
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            
            // 기본 로직으로 카테고리별 성과 분석
            List<NewsletterDelivery> allDeliveries = deliveryRepository.findByCreatedAtAfter(since);
            
            // 카테고리별 그룹화 및 성과 계산
            Map<String, List<NewsletterDelivery>> categoryGroups = new HashMap<>();
            
            for (NewsletterDelivery delivery : allDeliveries) {
                // 배송 기록에서 카테고리 정보를 가져오는 로직 (실제 구현에 따라 다름)
                String category = "GENERAL"; // 기본값
                categoryGroups.computeIfAbsent(category, k -> new ArrayList<>()).add(delivery);
            }
            
            List<CategoryPerformance> performances = categoryGroups.entrySet().stream()
                    .map(entry -> {
                        String category = entry.getKey();
                        List<NewsletterDelivery> deliveries = entry.getValue();
                        
                        long totalSent = deliveries.size();
                        long totalOpened = deliveries.stream()
                                .mapToLong(d -> d.getOpenedAt() != null ? 1 : 0)
                                .sum();
                        
                        double engagementRate = totalSent > 0 ? (double) totalOpened / totalSent * 100 : 0;
                        
                        return CategoryPerformance.builder()
                                .category(category)
                                .totalSent(totalSent)
                                .totalOpened(totalOpened)
                                .engagementRate(engagementRate)
                                .build();
                    })
                    .sorted((a, b) -> Double.compare(b.getEngagementRate(), a.getEngagementRate()))
                    .collect(Collectors.toList());
            
            return CategoryPerformanceAnalysis.builder()
                    .analysisPeriod(days)
                    .categoryPerformances(performances)
                    .topPerformingCategory(performances.isEmpty() ? null : performances.get(0).getCategory())
                    .averageEngagementRate(performances.stream()
                            .mapToDouble(CategoryPerformance::getEngagementRate)
                            .average()
                            .orElse(0.0))
                    .analysisDate(LocalDateTime.now())
                    .build();
                    
        } catch (Exception e) {
            log.error("카테고리 성과 분석 실패", e);
            throw new NewsletterException("카테고리 성과 분석 중 오류가 발생했습니다.", "CATEGORY_ANALYSIS_ERROR");
        }
    }

    /**
     * 실시간 대시보드 통계
     */
    @Transactional(readOnly = true)
    public RealTimeStats getRealTimeStats(int hours) {
        log.info("실시간 통계 조회: hours={}", hours);
        
        try {
            LocalDateTime since = LocalDateTime.now().minusHours(hours);
            
            // 기본 쿼리로 통계 계산
            List<NewsletterDelivery> recentDeliveries = deliveryRepository.findByCreatedAtAfter(since);
            
            long pendingCount = recentDeliveries.stream()
                    .mapToLong(d -> d.getStatus() == DeliveryStatus.PENDING ? 1 : 0)
                    .sum();
            
            long processingCount = recentDeliveries.stream()
                    .mapToLong(d -> d.getStatus() == DeliveryStatus.PROCESSING ? 1 : 0)
                    .sum();
            
            long sentCount = recentDeliveries.stream()
                    .mapToLong(d -> d.getStatus() == DeliveryStatus.SENT ? 1 : 0)
                    .sum();
            
            long openedCount = recentDeliveries.stream()
                    .mapToLong(d -> d.getOpenedAt() != null ? 1 : 0)
                    .sum();
            
            long failedCount = recentDeliveries.stream()
                    .mapToLong(d -> d.getStatus() == DeliveryStatus.FAILED ? 1 : 0)
                    .sum();
            
            long bouncedCount = recentDeliveries.stream()
                    .mapToLong(d -> d.getStatus() == DeliveryStatus.BOUNCED ? 1 : 0)
                    .sum();
            
            return RealTimeStats.builder()
                    .pendingCount(pendingCount)
                    .processingCount(processingCount)
                    .sentCount(sentCount)
                    .openedCount(openedCount)
                    .failedCount(failedCount)
                    .bouncedCount(bouncedCount)
                    .timestamp(LocalDateTime.now())
                    .analysisPeriodHours(hours)
                    .build();
                    
        } catch (Exception e) {
            log.error("실시간 통계 조회 실패", e);
            throw new NewsletterException("실시간 통계 조회 중 오류가 발생했습니다.", "STATS_ERROR");
        }
    }

    // ========================================
    // 6. 이메일 추적 기능
    // ========================================

    /**
     * 이메일 열람 추적
     */
    public void trackEmailOpen(Long deliveryId, String userAgent, String ipAddress) {
        log.info("이메일 열람 추적: deliveryId={}", deliveryId);
        
        try {
            NewsletterDelivery delivery = deliveryRepository.findById(deliveryId)
                    .orElseThrow(() -> new NewsletterException("발송 기록을 찾을 수 없습니다.", "DELIVERY_NOT_FOUND"));
            
            if (delivery.getOpenedAt() == null) {
                delivery.setOpenedAt(LocalDateTime.now());
                delivery.setUpdatedAt(LocalDateTime.now());
                deliveryRepository.save(delivery);
                
                // 사용자 행동 기록
                trackNewsView(delivery.getUserId(), delivery.getNewsletterId(), "EMAIL_NEWSLETTER");
            }
            
        } catch (Exception e) {
            log.error("이메일 열람 추적 실패: deliveryId={}", deliveryId, e);
            // 추적 실패는 사용자 경험에 영향을 주지 않도록 예외를 던지지 않음
        }
    }

    /**
     * 링크 클릭 추적
     */
    public void trackLinkClick(Long deliveryId, Long newsId, String linkUrl) {
        log.info("링크 클릭 추적: deliveryId={}, newsId={}, linkUrl={}", deliveryId, newsId, linkUrl);
        
        try {
            NewsletterDelivery delivery = deliveryRepository.findById(deliveryId)
                    .orElseThrow(() -> new NewsletterException("발송 기록을 찾을 수 없습니다.", "DELIVERY_NOT_FOUND"));
            
            // 클릭 기록
            trackNewsClick(delivery.getUserId(), newsId, "EMAIL_NEWSLETTER");
            
            // 첫 클릭인 경우 열람으로도 기록
            if (delivery.getOpenedAt() == null) {
                delivery.setOpenedAt(LocalDateTime.now());
                delivery.setUpdatedAt(LocalDateTime.now());
                deliveryRepository.save(delivery);
            }
            
        } catch (Exception e) {
            log.error("링크 클릭 추적 실패: deliveryId={}", deliveryId, e);
        }
    }

    // ========================================
    // 7. 사용자 행동 추적 기능
    // ========================================

    /**
     * 사용자 뉴스 조회 기록 (UserReadHistory 기반)
     */
    public void trackNewsView(Long userId, Long newsId, String category) {
        try {
            // UserReadHistoryService를 사용하여 읽음 기록 추가
            userReadHistoryService.addReadHistory(userId, newsId);
            log.debug("뉴스 조회 기록: userId={}, newsId={}, category={}", userId, newsId, category);
        } catch (Exception e) {
            log.error("뉴스 조회 기록 실패", e);
        }
    }

    /**
     * 사용자 뉴스 클릭 기록 (UserReadHistory 기반)
     */
    public void trackNewsClick(Long userId, Long newsId, String category) {
        try {
            // UserReadHistoryService를 사용하여 읽음 기록 추가 (클릭도 읽음으로 간주)
            userReadHistoryService.addReadHistory(userId, newsId);
            log.debug("뉴스 클릭 기록: userId={}, newsId={}, category={}", userId, newsId, category);
        } catch (Exception e) {
            log.error("뉴스 클릭 기록 실패", e);
        }
    }

    // ========================================
    // 8. 스케줄링 기능
    // ========================================

    /**
     * 예약된 뉴스레터 발송 처리 (매분 실행)
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void processScheduledDeliveries() {
        log.info("예약된 뉴스레터 발송 처리 시작");

        LocalDateTime now = LocalDateTime.now();
        List<NewsletterDelivery> scheduledDeliveries = deliveryRepository
                .findByStatusAndScheduledAtBefore(DeliveryStatus.PENDING, now);

        if (scheduledDeliveries.isEmpty()) {
            log.debug("처리할 예약된 뉴스레터가 없습니다.");
            return;
        }

        log.info("처리할 예약된 뉴스레터 수: {}", scheduledDeliveries.size());

        for (NewsletterDelivery delivery : scheduledDeliveries) {
            try {
                processScheduledDelivery(delivery);
            } catch (Exception e) {
                log.error("예약된 뉴스레터 처리 실패: ID={}, Error={}", delivery.getId(), e.getMessage());
                delivery.updateStatus(DeliveryStatus.FAILED);
                delivery.setErrorMessage(e.getMessage());
                delivery.setUpdatedAt(LocalDateTime.now());
                deliveryRepository.save(delivery);
            }
        }

        log.info("예약된 뉴스레터 발송 처리 완료");
    }

    /**
     * 실패한 발송 재시도 (매 30분마다 실행)
     */
    @Scheduled(fixedRate = 1800000)
    @Transactional
    public void retryFailedDeliveries() {
        log.info("실패한 발송 재시도 처리 시작");

        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        List<NewsletterDelivery> failedDeliveries = deliveryRepository
                .findByStatusAndUpdatedAtAfterAndRetryCountLessThan(DeliveryStatus.FAILED, cutoff, MAX_RETRY_COUNT);

        if (failedDeliveries.isEmpty()) {
            log.debug("재시도할 실패한 발송이 없습니다.");
            return;
        }

        log.info("재시도할 실패한 발송 수: {}", failedDeliveries.size());

        for (NewsletterDelivery delivery : failedDeliveries) {
            try {
                retryFailedDelivery(delivery);
            } catch (Exception e) {
                log.error("실패한 발송 재시도 중 오류: ID={}, Error={}", delivery.getId(), e.getMessage());
            }
        }

        log.info("실패한 발송 재시도 처리 완료");
    }

    // ========================================
    // 9. 조회 기능
    // ========================================

    /**
     * 발송 상태별 조회
     */
    @Transactional(readOnly = true)
    public Page<NewsletterDelivery> getDeliveriesByStatus(DeliveryStatus status, Pageable pageable) {
        log.info("발송 상태별 조회: status={}", status);
        return deliveryRepository.findByStatus(status, pageable);
    }

    /**
     * 사용자별 발송 이력 조회
     */
    @Transactional(readOnly = true)
    public Page<NewsletterDelivery> getDeliveriesByUser(Long userId, Pageable pageable) {
        log.info("사용자별 발송 이력 조회: userId={}", userId);
        return deliveryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * 뉴스레터별 발송 기록 조회
     */
    @Transactional(readOnly = true)
    public List<NewsletterDelivery> getDeliveriesByNewsletter(Long newsletterId) {
        log.info("뉴스레터별 발송 기록 조회: newsletterId={}", newsletterId);
        return deliveryRepository.findByNewsletterIdOrderByCreatedAtDesc(newsletterId);
    }

    /**
     * 사용 가능한 카테고리 조회
     */
    public List<String> getAvailableCategories() {
        log.info("사용 가능한 카테고리 조회");
        
        try {
            // NewsServiceClient의 getCategories API 사용
            ApiResponse<List<NewsCategory>> response = newsServiceClient.getCategories();
            if (response != null && response.getData() != null) {
                return response.getData().stream()
                        .map(NewsCategory::getCategoryName)
                        .collect(Collectors.toList());
            }
            return getDefaultCategories();
        } catch (Exception e) {
            log.warn("카테고리 조회 실패, 기본 카테고리 반환", e);
            return getDefaultCategories();
        }
    }

    /**
     * 구독 상태 목록 조회
     */
    public List<String> getSubscriptionStatuses() {
        return Arrays.stream(SubscriptionStatus.values())
                .map(SubscriptionStatus::name)
                .collect(Collectors.toList());
    }


    // ========================================
    // Private Helper Methods - 유틸리티
    // ========================================

    private void validateSubscriptionRequest(SubscriptionRequest request) {
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new NewsletterException("이메일은 필수입니다.", "INVALID_EMAIL");
        }
        
        if (request.getFrequency() == null) {
            throw new NewsletterException("발송 빈도는 필수입니다.", "INVALID_FREQUENCY");
        }
    }

    private void updateSubscriptionFromRequest(Subscription subscription, SubscriptionRequest request, Long userId) {
        LocalDateTime now = LocalDateTime.now();
        
        if (subscription.getId() == null) {
            // 새 구독
            subscription.setUserId(userId);
            subscription.setSubscribedAt(now);
            subscription.setCreatedAt(now);
        }
        
        String categoriesJson = convertNewsCategoriesToJson(request.getPreferredCategories());
        log.debug("구독 업데이트: userId={}, requestCategories={}, jsonCategories={}", 
                userId, request.getPreferredCategories(), categoriesJson);
        
        subscription.setEmail(request.getEmail());
        subscription.setFrequency(request.getFrequency());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPreferredCategories(categoriesJson);
        subscription.setKeywords(convertToJson(request.getKeywords()));
        subscription.setSendTime(request.getSendTime() != null ? request.getSendTime() : 9);
        subscription.setPersonalized(request.isPersonalized());
        subscription.setUpdatedAt(now);
    }

    private Subscription getSubscriptionWithPermissionCheck(Long subscriptionId, Long userId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new NewsletterException("구독을 찾을 수 없습니다.", "SUBSCRIPTION_NOT_FOUND"));
        
        if (!subscription.getUserId().equals(userId)) {
            throw new NewsletterException("권한이 없습니다.", "UNAUTHORIZED");
        }
        
        return subscription;
    }

    private NewsletterDelivery getDeliveryWithPermissionCheck(Long deliveryId, Long userId) {
        NewsletterDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NewsletterException("발송 기록을 찾을 수 없습니다.", "DELIVERY_NOT_FOUND"));
        
        if (!delivery.getUserId().equals(userId)) {
            throw new NewsletterException("권한이 없습니다.", "UNAUTHORIZED");
        }
        
        return delivery;
    }

    private SubscriptionStatus validateAndParseStatus(String statusStr) {
        try {
            return SubscriptionStatus.valueOf(statusStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NewsletterException("유효하지 않은 구독 상태입니다.", "INVALID_STATUS");
        }
    }

    // ========================================
    // Private Helper Methods - 뉴스 수집 (수정된 API 호출)
    // ========================================

    private List<NewsResponse> collectNewsContent(List<CategoryResponse> preferences, boolean hasPreferences) {
        List<NewsResponse> selectedNews = new ArrayList<>();

        if (hasPreferences) {
            for (CategoryResponse category : preferences) {
                if (selectedNews.size() >= MAX_ITEMS) break;
                
                try {
                    List<NewsResponse> categoryNews = fetchNewsByCategory(category.getName(), 0, PER_CATEGORY_LIMIT);
                    appendDistinct(selectedNews, categoryNews, MAX_ITEMS);
                } catch (Exception e) {
                    log.warn("카테고리 {} 뉴스 수집 실패: {}", category.getName(), e.getMessage());
                }
            }
        }

        // 남은 슬롯을 인기/최신 뉴스로 채움
        fillRemainingSlots(selectedNews);

        return selectedNews;
    }

    private void fillRemainingSlots(List<NewsResponse> selectedNews) {
        if (selectedNews.size() < MAX_ITEMS) {
            try {
                List<NewsResponse> popular = fetchPopularNews(MAX_ITEMS - selectedNews.size());
                appendDistinct(selectedNews, popular, MAX_ITEMS);
            } catch (Exception e) {
                log.warn("인기 뉴스 수집 실패: {}", e.getMessage());
            }
        }
        
        if (selectedNews.size() < MAX_ITEMS) {
            try {
                List<NewsResponse> latest = fetchLatestNews(null, MAX_ITEMS - selectedNews.size());
                appendDistinct(selectedNews, latest, MAX_ITEMS);
            } catch (Exception e) {
                log.warn("최신 뉴스 수집 실패: {}", e.getMessage());
            }
        }
    }

    private List<CategoryResponse> getUserPreferences(Long userId) {
        try {
            ApiResponse<List<CategoryResponse>> response = userServiceClient.getUserPreferences(userId);
            if (response != null && response.getData() != null && !response.getData().isEmpty()) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("사용자 선호 카테고리 조회 실패: {}", e.getMessage());
        }
        return new ArrayList<>();
    }

    private List<NewsResponse> fetchPopularNews(int size) {
        try {
            ApiResponse<List<NewsResponse>> response = newsServiceClient.getPopularNews(size);
            return response != null && response.getData() != null ? response.getData() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("인기 뉴스 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<NewsResponse> fetchTrendingNews(int hours, int limit) {
        try {
            ApiResponse<List<NewsResponse>> response = newsServiceClient.getTrendingNews(hours, limit);
            return response != null && response.getData() != null ? response.getData() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("트렌딩 뉴스 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<NewsResponse> fetchLatestNews(List<String> categories, int limit) {
        try {
            ApiResponse<List<NewsResponse>> response = newsServiceClient.getLatestNews(categories, limit);
            return response != null && response.getData() != null ? response.getData() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("최신 뉴스 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private void appendDistinct(List<NewsResponse> accumulator, List<NewsResponse> candidates, int maxSize) {
        if (candidates == null || candidates.isEmpty()) return;
        
        Set<Long> existingIds = accumulator.stream()
                .map(NewsResponse::getId)
                .collect(Collectors.toSet());
        
        for (NewsResponse news : candidates) {
            if (accumulator.size() >= maxSize) break;
            if (existingIds.add(news.getId())) {
                accumulator.add(news);
            }
        }
    }

    // ========================================
    // Private Helper Methods - 콘텐츠 생성
    // ========================================

    private String generateTitle(boolean isPersonalized) {
        return isPersonalized ? "당신을 위한 맞춤 뉴스레터" : "오늘의 핫한 뉴스";
    }

    private List<NewsletterContent.Section> buildSections(List<NewsResponse> newsList, boolean isPersonalized) {
        List<NewsletterContent.Section> sections = new ArrayList<>();
        
        if (newsList.isEmpty()) {
            sections.add(createEmptySection());
        } else {
            sections.add(createMainSection(newsList, isPersonalized));
        }
        
        return sections;
    }

    private List<NewsletterContent.Section> buildCategorySections(List<NewsResponse> newsList, String category) {
        List<NewsletterContent.Section> sections = new ArrayList<>();
        
        if (newsList.isEmpty()) {
            sections.add(createEmptySection());
        } else {
            List<NewsletterContent.Article> articles = newsList.stream()
                    .map(this::toContentArticle)
                    .collect(Collectors.toList());
            
            sections.add(NewsletterContent.Section.builder()
                    .heading(category + " 뉴스")
                    .sectionType("CATEGORY")
                    .description(category + " 카테고리의 최신 뉴스입니다.")
                    .articles(articles)
                    .build());
        }
        
        return sections;
    }

    private NewsletterContent.Section createEmptySection() {
        return NewsletterContent.Section.builder()
                .heading("오늘의 뉴스")
                .sectionType("DEFAULT")
                .description("현재 뉴스를 불러올 수 없습니다.")
                .articles(new ArrayList<>())
                .build();
    }

    private NewsletterContent.Section createMainSection(List<NewsResponse> newsList, boolean isPersonalized) {
        List<NewsletterContent.Article> articles = newsList.stream()
                .map(this::toContentArticle)
                .collect(Collectors.toList());
        
        return NewsletterContent.Section.builder()
                .heading(isPersonalized ? "당신을 위한 뉴스" : "오늘의 뉴스")
                .sectionType(isPersonalized ? "PERSONALIZED" : "TRENDING")
                .description(isPersonalized ? "관심 카테고리 기반으로 선별된 뉴스입니다." : "현재 인기 있는 뉴스입니다.")
                .articles(articles)
                .build();
    }

    private NewsletterContent.Article toContentArticle(NewsResponse news) {
        return NewsletterContent.Article.builder()
                .id(news.getId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .category(news.getCategory())
                .url(news.getSourceUrl())
                .publishedAt(news.getPublishedAt())
                .imageUrl(news.getImageUrl())
                .viewCount(null)
                .shareCount(null)
                .personalizedScore(1.0)
                .build();
    }

    // ========================================
    // Private Helper Methods - 발송 처리
    // ========================================

    private DeliveryStats processDeliveryRequest(NewsletterDeliveryRequest request, boolean isScheduled) {
        int totalTargets = request.getTargetUserIds().size();
        int successCount = 0;
        int failureCount = 0;
        
        for (Long targetUserId : request.getTargetUserIds()) {
            try {
                NewsletterDelivery delivery = createDeliveryRecord(request, targetUserId, isScheduled);
                deliveryRepository.save(delivery);
                
                if (!isScheduled) {
                    performDelivery(delivery);
                }
                
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("사용자 {} 발송 처리 실패: {}", targetUserId, e.getMessage());
            }
        }
        
        double successRate = totalTargets > 0 ? (double) successCount / totalTargets * 100 : 0.0;
        return DeliveryStats.builder()
                .totalSent(successCount)
                .totalFailed(failureCount)
                .totalScheduled(totalTargets)
                .successRate(successRate)
                .build();
    }

    private NewsletterDelivery createDeliveryRecord(NewsletterDeliveryRequest request, Long userId, boolean isScheduled) {
        return NewsletterDelivery.builder()
                .userId(userId)
                .newsletterId(request.getNewsletterId())
                .deliveryMethod(request.getDeliveryMethod())
                .status(isScheduled ? DeliveryStatus.SCHEDULED : DeliveryStatus.PROCESSING)
                .scheduledAt(isScheduled ? request.getScheduledAt() : LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private void performDelivery(NewsletterDelivery delivery) {
        try {
            log.info("뉴스레터 발송 수행: ID={}, Method={}", delivery.getId(), delivery.getDeliveryMethod());

            switch (delivery.getDeliveryMethod()) {
                case EMAIL -> sendByEmail(delivery);
                case SMS -> sendBySms(delivery);
                case PUSH -> sendByPushNotification(delivery);
            }

            delivery.updateStatus(DeliveryStatus.SENT);
            delivery.setSentAt(LocalDateTime.now());

        } catch (Exception e) {
            log.error("뉴스레터 발송 실패: ID={}, Error={}", delivery.getId(), e.getMessage());
            delivery.updateStatus(DeliveryStatus.FAILED);
            delivery.setErrorMessage(e.getMessage());
        }

        delivery.setUpdatedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);
    }

    private void sendByEmail(NewsletterDelivery delivery) {
        try {
            log.info("이메일 발송: userId={}, newsletterId={}", delivery.getUserId(), delivery.getNewsletterId());
            
            NewsletterContent content = buildPersonalizedContent(delivery.getUserId(), delivery.getNewsletterId());
            String htmlContent = emailRenderer.renderToHtml(content);
            
            // TODO: 실제 이메일 발송 서비스 호출
            // emailService.sendHtmlEmail(delivery.getUserId(), content.getTitle(), htmlContent);
            
            log.info("이메일 콘텐츠 생성 완료: userId={}", delivery.getUserId());
            
        } catch (Exception e) {
            log.error("이메일 발송 실패: deliveryId={}", delivery.getId(), e);
            throw new RuntimeException("이메일 발송 실패", e);
        }
    }

    private void sendBySms(NewsletterDelivery delivery) {
        log.info("SMS 발송: userId={}", delivery.getUserId());
        // TODO: SMS 발송 로직 구현
        throw new RuntimeException("SMS 발송 기능은 아직 구현되지 않았습니다.");
    }

    private void sendByPushNotification(NewsletterDelivery delivery) {
        log.info("푸시 알림 발송: userId={}", delivery.getUserId());
        // TODO: 푸시 알림 발송 로직 구현
        throw new RuntimeException("푸시 알림 발송 기능은 아직 구현되지 않았습니다.");
    }

    @Async
    @Transactional
    private void processScheduledDelivery(NewsletterDelivery delivery) {
        log.info("예약된 뉴스레터 처리: ID={}", delivery.getId());

        delivery.updateStatus(DeliveryStatus.PROCESSING);
        delivery.setUpdatedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);

        performDelivery(delivery);
    }

    private void retryFailedDelivery(NewsletterDelivery delivery) {
        log.info("실패한 발송 재시도: ID={}", delivery.getId());

        delivery.updateStatus(DeliveryStatus.PROCESSING);
        delivery.incrementRetryCount();
        delivery.setUpdatedAt(LocalDateTime.now());
        deliveryRepository.save(delivery);

        performDelivery(delivery);
    }

    // ========================================
    // Private Helper Methods - 검색 및 필터링
    // ========================================

    private boolean matchesFilter(NewsResponse news, NewsFilterRequest request) {
        // 카테고리 필터
        if (request.getCategories() != null && !request.getCategories().isEmpty()) {
            if (!request.getCategories().contains(news.getCategory())) {
                return false;
            }
        }
        
        // 날짜 필터
        if (request.getStartDate() != null && request.getEndDate() != null) {
            LocalDateTime newsDate = news.getPublishedAt();
            if (newsDate.isBefore(request.getStartDate()) || 
                newsDate.isAfter(request.getEndDate())) {
                return false;
            }
        }
        
        // 키워드 필터
        if (request.getKeywords() != null && !request.getKeywords().isEmpty()) {
            String title = news.getTitle().toLowerCase();
            String summary = news.getSummary() != null ? news.getSummary().toLowerCase() : "";
            
            boolean containsKeyword = request.getKeywords().stream()
                    .anyMatch(keyword -> title.contains(keyword.toLowerCase()) || 
                                       summary.contains(keyword.toLowerCase()));
            
            if (!containsKeyword) {
                return false;
            }
        }
        
        return true;
    }

    private String extractTopic(String title) {
        // 간단한 토픽 추출 로직 (실제로는 더 정교한 알고리즘 필요)
        String[] words = title.split(" ");
        return words.length > 0 ? words[0] : "일반";
    }

    private EnhancedNewsletterInfo convertToEnhancedInfo(NewsResponse news) {
        return EnhancedNewsletterInfo.builder()
                .id(news.getId())
                .title(news.getTitle())
                .summary(news.getSummary())
                .category(news.getCategory())
                .publishedAt(news.getPublishedAt())
                .sourceUrl(news.getSourceUrl())
                .imageUrl(news.getImageUrl())
                .estimatedReadTime(calculateReadTime(news.getSummary()))
                .relevanceScore(1.0)
                .build();
    }

    private int calculateReadTime(String content) {
        if (content == null || content.isEmpty()) return 1;
        int wordCount = content.split("\\s+").length;
        return Math.max(1, wordCount / 200); // 분당 200단어 기준
    }

    // ========================================
    // Private Helper Methods - 추천 시스템
    // ========================================

    private Map<String, Double> calculateCategoryScores(List<CategoryResponse> preferences, 
                                                       List<ReadHistoryResponse> readHistory) {
        Map<String, Double> scores = new HashMap<>();
        
        // 선호 카테고리 점수
        for (CategoryResponse pref : preferences) {
            scores.put(pref.getName(), 1.0);
        }
        
        // 읽은 뉴스 기반 점수 조정
        Map<String, Long> categoryReadCounts = readHistory.stream()
                .filter(history -> history.getCategoryName() != null)
                .collect(Collectors.groupingBy(
                        ReadHistoryResponse::getCategoryName,
                        Collectors.counting()
                ));
        
        for (Map.Entry<String, Long> entry : categoryReadCounts.entrySet()) {
            double readScore = Math.log(entry.getValue() + 1) * 0.1;
            scores.merge(entry.getKey(), readScore, Double::sum);
        }
        
        return scores;
    }

    private List<NewsResponse> fetchRecommendationCandidates(Map<String, Double> categoryScores, int limit) {
        List<NewsResponse> candidates = new ArrayList<>();
        
        for (String category : categoryScores.keySet()) {
            try {
                List<NewsResponse> categoryNews = fetchNewsByCategory(category, 0, limit / categoryScores.size() + 1);
                candidates.addAll(categoryNews);
            } catch (Exception e) {
                log.warn("추천 후보 수집 실패: category={}", category, e);
            }
        }
        
        return candidates;
    }

    private double calculatePersonalizationScore(NewsResponse news, Map<String, Double> categoryScores) {
        double baseScore = categoryScores.getOrDefault(news.getCategory(), 0.0);
        
        // 시간 점수 (최신일수록 높음)
        long hoursAgo = ChronoUnit.HOURS.between(news.getPublishedAt(), LocalDateTime.now());
        double timeScore = Math.max(0, 1.0 - (hoursAgo / 24.0 * 0.1));
        
        return baseScore + timeScore;
    }

    private String generateRecommendationReason(NewsResponse news, Map<String, Double> categoryScores) {
        if (categoryScores.containsKey(news.getCategory())) {
            return news.getCategory() + " 카테고리에 관심을 보이셨습니다";
        }
        return "최신 트렌드 뉴스입니다";
    }

    private double calculateTrendScore(NewsResponse news) {
        // 간단한 트렌드 점수 계산 (실제로는 조회수, 공유수, 댓글 수 등을 고려)
        long hoursAgo = ChronoUnit.HOURS.between(news.getPublishedAt(), LocalDateTime.now());
        return Math.max(0, 100.0 - (hoursAgo * 2.0));
    }

    // ========================================
    // Private Helper Methods - 분석
    // ========================================

    private UserEngagement createEmptyEngagement(Long userId) {
        return UserEngagement.builder()
                .userId(userId)
                .engagementRate(0.0)
                .recommendation("데이터가 부족합니다. 더 많은 뉴스레터를 받아보세요.")
                .build();
    }

    private String generateEngagementRecommendation(double engagementRate, Double avgOpenDelay, long totalReceived) {
        if (engagementRate > HIGH_ENGAGEMENT_THRESHOLD) {
            return "매우 높은 참여도입니다! 개인화를 더욱 강화하거나 발송 빈도를 늘려보세요.";
        } else if (engagementRate > MEDIUM_ENGAGEMENT_THRESHOLD) {
            return "좋은 참여도입니다. 현재 설정을 유지하시면 됩니다.";
        } else if (engagementRate > LOW_ENGAGEMENT_THRESHOLD) {
            return "참여도가 보통 수준입니다. 콘텐츠 품질을 개선하거나 발송 시간을 조정해보세요.";
        } else {
            return "참여도가 낮습니다. 구독 빈도를 줄이거나 관심 키워드를 재설정해보세요.";
        }
    }

    private int calculateMostActiveHour(List<ReadHistoryResponse> readHistory) {
        if (readHistory.isEmpty()) return 9; // 기본값
        
        Map<Integer, Long> hourCounts = readHistory.stream()
                .collect(Collectors.groupingBy(
                        history -> history.getUpdatedAt().getHour(),
                        Collectors.counting()
                ));
        
        return hourCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(9);
    }

    private List<Double> calculateEngagementTrend(Long userId, int days) {
        // 간단한 트렌드 계산 (일주일 단위)
        List<Double> trend = new ArrayList<>();
        int weekCount = Math.max(1, days / 7);
        
        for (int i = 0; i < weekCount; i++) {
            LocalDateTime weekStart = LocalDateTime.now().minusWeeks(i + 1);
            LocalDateTime weekEnd = LocalDateTime.now().minusWeeks(i);
            
            List<NewsletterDelivery> weekDeliveries = deliveryRepository
                    .findByUserIdAndCreatedAtBetween(userId, weekStart, weekEnd);
            
            long opened = weekDeliveries.stream()
                    .mapToLong(d -> d.getOpenedAt() != null ? 1 : 0)
                    .sum();
            
            double weeklyRate = weekDeliveries.size() > 0 ? 
                    (double) opened / weekDeliveries.size() * 100 : 0;
            
            trend.add(0, weeklyRate); // 역순으로 추가 (오래된 것부터)
        }
        
        return trend;
    }

    // ========================================
    // Private Helper Methods - 헬스체크
    // ========================================

    private boolean checkDatabaseHealth() {
        try {
            subscriptionRepository.count();
            return true;
        } catch (Exception e) {
            log.error("데이터베이스 헬스체크 실패", e);
            return false;
        }
    }

    private boolean checkNewsServiceHealth() {
        try {
            newsServiceClient.getLatestNews(null, 1);
            return true;
        } catch (Exception e) {
            log.error("뉴스 서비스 헬스체크 실패", e);
            return false;
        }
    }

    private boolean checkUserServiceHealth() {
        try {
            userServiceClient.getActiveUsers(0, 1);
            return true;
        } catch (Exception e) {
            log.error("사용자 서비스 헬스체크 실패", e);
            return false;
        }
    }

    // ========================================
    // Private Helper Methods - 데이터 변환
    // ========================================

    private SubscriptionResponse convertToSubscriptionResponse(Subscription subscription) {
        List<NewsCategory> categories = parseJsonToCategories(subscription.getPreferredCategories());
        log.debug("구독 응답 변환: subscriptionId={}, rawCategories={}, parsedCategories={}", 
                subscription.getId(), subscription.getPreferredCategories(), categories);
        
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUserId())
                .email(subscription.getEmail())
                .preferredCategories(categories)
                .keywords(parseJsonToStringList(subscription.getKeywords()))
                .frequency(subscription.getFrequency())
                .status(subscription.getStatus())
                .sendTime(subscription.getSendTime())
                .isPersonalized(subscription.isPersonalized())
                .subscribedAt(subscription.getSubscribedAt())
                .lastSentAt(subscription.getLastSentAt())
                .createdAt(subscription.getCreatedAt())
                .build();
    }



    private String convertToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(list);
        } catch (Exception e) {
            log.error("리스트 JSON 변환 실패", e);
            return "[]";
        }
    }

    private List<NewsCategory> parseJsonToCategories(String json) {
        if (json == null || json.trim().isEmpty() || "[]".equals(json)) {
            return new ArrayList<>();
        }
        try {
            // 먼저 문자열 리스트로 파싱
            com.fasterxml.jackson.core.type.TypeReference<List<String>> stringTypeRef = 
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {};
            List<String> categoryNames = new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, stringTypeRef);
            log.debug("JSON 파싱 결과: json={}, categoryNames={}", json, categoryNames);
            
            // 문자열을 NewsCategory enum으로 변환 (기존 데이터 호환성을 위해 유연하게 처리)
            List<NewsCategory> result = categoryNames.stream()
                    .map(name -> {
                        try {
                            // 1. 먼저 enum name으로 직접 매칭 시도
                            NewsCategory category = NewsCategory.valueOf(name);
                            log.debug("enum name으로 매칭 성공: {} -> {}", name, category);
                            return category;
                        } catch (IllegalArgumentException e1) {
                            try {
                                // 2. categoryName으로 매칭 시도 (기존 데이터 호환성)
                                for (NewsCategory category : NewsCategory.values()) {
                                    if (category.getCategoryName().equals(name)) {
                                        log.debug("categoryName으로 매칭 성공: {} -> {}", name, category);
                                        return category;
                                    }
                                }
                                log.warn("알 수 없는 카테고리: {}", name);
                                return null;
                            } catch (Exception e2) {
                                log.warn("카테고리 매칭 실패: {}", name);
                                return null;
                            }
                        }
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            
            log.debug("최종 파싱 결과: {}", result);
            return result;
        } catch (Exception e) {
            log.error("카테고리 JSON 파싱 실패: json={}", json, e);
            return new ArrayList<>();
        }
    }

    private List<String> parseJsonToStringList(String json) {
        if (json == null || json.trim().isEmpty() || "[]".equals(json)) {
            return new ArrayList<>();
        }
        try {
            com.fasterxml.jackson.core.type.TypeReference<List<String>> typeRef = 
                    new com.fasterxml.jackson.core.type.TypeReference<List<String>>() {};
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(json, typeRef);
        } catch (Exception e) {
            log.error("문자열 리스트 JSON 파싱 실패", e);
            return new ArrayList<>();
        }
    }

    // ========================================
    // Private Helper Methods - 추가 유틸리티
    // ========================================

    private List<String> getDefaultCategories() {
        return Arrays.asList(
                NewsCategory.POLITICS.getCategoryName(),
                NewsCategory.ECONOMY.getCategoryName(),
                NewsCategory.SOCIETY.getCategoryName(),
                NewsCategory.LIFE.getCategoryName(),
                NewsCategory.INTERNATIONAL.getCategoryName(),
                NewsCategory.IT_SCIENCE.getCategoryName(),
                NewsCategory.VEHICLE.getCategoryName(),
                NewsCategory.TRAVEL_FOOD.getCategoryName(),
                NewsCategory.ART.getCategoryName()
        );
    }

    // ========================================
    // Private Helper Methods - UserReadHistory 기반 (새로 추가)
    // ========================================

    private Map<String, Double> calculateCategoryScoresFromReadHistory(List<CategoryResponse> preferences, List<ReadHistoryResponse> readHistory) {
        Map<String, Double> scores = new HashMap<>();
        
        // 선호 카테고리 점수
        for (CategoryResponse pref : preferences) {
            scores.put(pref.getName(), 1.0);
        }
        
        // 읽은 뉴스 기반 점수 조정
        Map<String, Long> categoryReadCounts = readHistory.stream()
                .filter(history -> history.getCategoryName() != null)
                .collect(Collectors.groupingBy(
                        ReadHistoryResponse::getCategoryName,
                        Collectors.counting()
                ));
        
        for (Map.Entry<String, Long> entry : categoryReadCounts.entrySet()) {
            double readScore = Math.log(entry.getValue() + 1) * 0.1;
            scores.merge(entry.getKey(), readScore, Double::sum);
        }
        
        return scores;
    }

    private int calculateMostActiveHourFromReadHistory(List<ReadHistoryResponse> readHistory) {
        if (readHistory.isEmpty()) return 9; // 기본값
        
        Map<Integer, Long> hourCounts = readHistory.stream()
                .collect(Collectors.groupingBy(
                        history -> history.getUpdatedAt().getHour(),
                        Collectors.counting()
                ));
        
        return hourCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(9);
    }


    /**
     * @deprecated 대신 scheduleNewsletter()를 사용하세요
     */
    @Deprecated
    public NewsletterDelivery scheduleDelivery(NewsletterDelivery requestDTO) {
        log.warn("Deprecated method scheduleDelivery() called. Use scheduleNewsletter() instead.");
        
        NewsletterDelivery delivery = NewsletterDelivery.builder()
                .newsletterId(requestDTO.getNewsletterId())
                .userId(requestDTO.getUserId())
                .status(DeliveryStatus.PENDING)
                .deliveryMethod(requestDTO.getDeliveryMethod())
                .scheduledAt(requestDTO.getScheduledAt())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return deliveryRepository.save(delivery);
    }

    /**
     * @deprecated 대신 sendNewsletterNow()를 사용하세요
     */
    @Deprecated
    public NewsletterDelivery sendImmediately(NewsletterDelivery requestDTO) {
        log.warn("Deprecated method sendImmediately() called. Use sendNewsletterNow() instead.");
        
        NewsletterDelivery delivery = NewsletterDelivery.builder()
                .newsletterId(requestDTO.getNewsletterId())
                .userId(requestDTO.getUserId())
                .status(DeliveryStatus.PENDING)
                .deliveryMethod(requestDTO.getDeliveryMethod())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        NewsletterDelivery saved = deliveryRepository.save(delivery);
        performDelivery(saved);

        return saved;
    }

    /**
     * @deprecated 대신 cancelDelivery(Long deliveryId, Long userId)를 사용하세요
     */
    @Deprecated
    public NewsletterDelivery cancelDelivery(Long deliveryId) {
        log.warn("Deprecated method cancelDelivery(Long) called. Use cancelDelivery(Long, Long) instead.");
        
        NewsletterDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NewsletterException("발송 정보를 찾을 수 없습니다.", "DELIVERY_NOT_FOUND"));

        if (delivery.getStatus() == DeliveryStatus.SENT) {
            throw new NewsletterException("이미 발송된 뉴스레터는 취소할 수 없습니다.", "ALREADY_SENT");
        }

        delivery.updateStatus(DeliveryStatus.BOUNCED);
        delivery.setUpdatedAt(LocalDateTime.now());
        return deliveryRepository.save(delivery);
    }

    // ========================================
    // Helper Methods - 뉴스레터 생성 관련
    // ========================================

    private String getNewsletterDisplayName(String newsletterType) {
        return switch (newsletterType) {
            case "MORNING_BRIEFING" -> "모닝 브리핑";
            case "WEEKLY_HIGHLIGHTS" -> "주간 하이라이트";
            case "ECONOMY_WEEKLY" -> "경제 트렌드 위클리";
            case "TECH_INSIGHTS" -> "IT/과학 인사이드";
            case "VIRAL_NEWS" -> "바이럴 뉴스";
            default -> "뉴스레터";
        };
    }

    private String generateCategoryDescription(NewsCategory category) {
        return switch (category) {
            case POLITICS -> "매일 아침 정치 소식을 간결하게 정리해드립니다";
            case ECONOMY -> "중요 경제 지표, 주식 시장 동향, 투자 인사이트를 제공합니다";
            case SOCIETY -> "사회 각 분야의 주요 이슈를 균형 있게 다룹니다";
            case LIFE -> "문화, 생활, 트렌드 소식을 재미있게 전달합니다";
            case INTERNATIONAL -> "전 세계 주요 국제뉴스와 글로벌 이슈를 분석합니다";
            case IT_SCIENCE -> "최신 IT 기술과 과학 발견을 쉽게 설명합니다";
            case VEHICLE -> "자동차 산업, 교통 정책, 모빌리티 트렌드를 다룹니다";
            case TRAVEL_FOOD -> "여행 정보, 맛집 소식, 라이프스타일 콘텐츠를 제공합니다";
            case ART -> "예술, 전시, 문화 행사 소식을 전문적으로 큐레이션합니다";
        };
    }

    private List<String> generateCategoryTags(NewsCategory category) {
        return switch (category) {
            case POLITICS -> Arrays.asList("정치", "정책", "국정감사", "선거");
            case ECONOMY -> Arrays.asList("경제전망", "주식", "부동산", "투자");
            case SOCIETY -> Arrays.asList("사회현안", "교육", "복지", "안전");
            case  LIFE  -> Arrays.asList("문화", "예술", "엔터테인먼트", "라이프스타일");
            case INTERNATIONAL -> Arrays.asList("국제뉴스", "외교", "글로벌", "해외");
            case IT_SCIENCE -> Arrays.asList("IT", "과학", "기술", "혁신");
            case VEHICLE -> Arrays.asList("자동차", "교통", "모빌리티", "전기차");
            case TRAVEL_FOOD -> Arrays.asList("여행", "맛집", "요리", "문화체험");
            case ART -> Arrays.asList("예술", "전시", "공연", "문화행사");
        };
    }

    private String generateSampleContent(NewsCategory category) {
        return switch (category) {
            case POLITICS -> "오늘의 주요 정치 뉴스 3건과 국정감사 핵심 이슈를 5분만에 읽어보세요.";
            case ECONOMY -> "이번 주 증시 전망과 부동산 정책 변화, 글로벌 경제 동향을 분석합니다.";
            case SOCIETY -> "교육 정책 변화, 복지 제도 개선, 사회 안전망 강화 소식을 전해드립니다.";
            case LIFE -> "주말 문화 행사 추천, 화제의 전시회, 새로운 트렌드를 소개합니다.";
            case INTERNATIONAL -> "미중 관계, 유럽 정세, 아시아 경제 협력 등 글로벌 이슈를 다룹니다.";
            case IT_SCIENCE -> "AI 기술 발전, 우주 탐사, 바이오 기술 등 최신 과학 소식을 전합니다.";
            case VEHICLE -> "자율주행차 발전, 전기차 시장 동향, 교통 정책 변화를 다룹니다.";
            case TRAVEL_FOOD -> "국내외 여행지 추천, 계절별 맛집, 요리 트렌드를 소개합니다.";
            case ART -> "국내외 주요 전시회, 공연 정보, 예술가 인터뷰를 제공합니다.";
        };
    }

    private boolean isRecommendedCategory(NewsCategory category) {
        // 인기 카테고리를 추천으로 표시
        return Arrays.asList(NewsCategory.POLITICS, NewsCategory.ECONOMY, 
                           NewsCategory.IT_SCIENCE, NewsCategory.INTERNATIONAL)
                     .contains(category);
    }

    private int getRecentNewsCount(String categoryName) {
        try {
            ApiResponse<List<NewsResponse>> response = newsServiceClient.getNewsByCategory(categoryName, 0, 100);
            return response != null && response.getData() != null ? response.getData().size() : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private double calculateCategoryEngagement(String categoryName) {
        // 실제로는 더 복잡한 계산이 필요하지만, 간단히 구현
        return 4.0 + (Math.random() * 1.0); // 4.0-5.0 범위의 랜덤값
    }

    private long getSubscriberCount(String newsletterType) {
        return subscriptionRepository.countByPreferredCategoriesContainingAndStatus(newsletterType, SubscriptionStatus.ACTIVE);
    }

    // ========================================
    // Helper Methods - 개인화 관련
    // ========================================

    private UserPreferenceProfile analyzeUserPreferences(Long userId) {
        // 1. 사용자 선호 카테고리
        List<CategoryResponse> preferences = getUserPreferences(userId);
        
        // 2. 최근 읽기 기록 분석
        List<ReadHistoryResponse> recentReadHistory = userReadHistoryService.getRecentReadHistory(userId, 30);
        
        // 3. 시간대별 활동 패턴
        Map<Integer, Integer> hourlyActivity = analyzeHourlyActivity(recentReadHistory);
        
        // 4. 선호하는 뉴스 길이 분석
        int preferredReadTime = analyzePreferredReadTime(recentReadHistory);
        
        return UserPreferenceProfile.builder()
                .userId(userId)
                .preferredCategories(preferences.stream()
                        .map(CategoryResponse::getName)
                        .collect(Collectors.toList()))
                .recentInteractions(recentReadHistory.size())
                .mostActiveHour(findMostActiveHour(hourlyActivity))
                .preferredReadTime(preferredReadTime)
                .engagementScore(calculateUserEngagementScore(recentReadHistory))
                .lastAnalyzed(LocalDateTime.now())
                .build();
    }

    private ContentStrategy determineContentStrategy(String newsletterType, UserPreferenceProfile profile) {
        return switch (newsletterType) {
            case "MORNING_BRIEFING" -> ContentStrategy.builder()
                    .maxArticles(5)
                    .preferredLength("SHORT")
                    .prioritizeRecent(true)
                    .includeBreaking(true)
                    .tone("INFORMATIVE")
                    .build();
                    
            case "WEEKLY_HIGHLIGHTS" -> ContentStrategy.builder()
                    .maxArticles(10)
                    .preferredLength("MEDIUM")
                    .prioritizeRecent(false)
                    .includeAnalysis(true)
                    .tone("COMPREHENSIVE")
                    .build();
                    
            case "TECH_INSIGHTS" -> ContentStrategy.builder()
                    .maxArticles(6)
                    .preferredLength("LONG")
                    .prioritizeRecent(true)
                    .includeExpert(true)
                    .tone("TECHNICAL")
                    .build();
                    
            default -> ContentStrategy.builder()
                    .maxArticles(8)
                    .preferredLength("MEDIUM")
                    .prioritizeRecent(true)
                    .tone("BALANCED")
                    .build();
        };
    }

    private List<NewsResponse> collectPersonalizedNews(UserPreferenceProfile profile, ContentStrategy strategy) {
        List<NewsResponse> collectedNews = new ArrayList<>();
        
        // 1. 선호 카테고리 기반 뉴스 수집
        for (String category : profile.getPreferredCategories()) {
            if (collectedNews.size() >= strategy.getMaxArticles()) break;
            
            try {
                List<NewsResponse> categoryNews = fetchNewsByCategory(category, 0, 
                        Math.min(3, strategy.getMaxArticles() - collectedNews.size()));
                collectedNews.addAll(categoryNews);
            } catch (Exception e) {
                log.warn("카테고리 {} 뉴스 수집 실패", category);
            }
        }
        
        // 2. 트렌딩 뉴스로 보완
        if (collectedNews.size() < strategy.getMaxArticles()) {
            try {
                ApiResponse<List<NewsResponse>> trendingResponse = newsServiceClient.getTrendingNews(24, 
                        strategy.getMaxArticles() - collectedNews.size());
                if (trendingResponse != null && trendingResponse.getData() != null) {
                    collectedNews.addAll(trendingResponse.getData());
                }
            } catch (Exception e) {
                log.warn("트렌딩 뉴스 수집 실패");
            }
        }
        
        return collectedNews.stream()
                .distinct()
                .limit(strategy.getMaxArticles())
                .collect(Collectors.toList());
    }

    private List<NewsletterContent.Section> buildOptimizedSections(List<NewsResponse> news, 
            ContentStrategy strategy, 
            UserPreferenceProfile profile) {
        List<NewsletterContent.Section> sections = new ArrayList<>();
        
        // 1. 주요 뉴스 섹션
        List<NewsletterContent.Article> topNews = news.stream().limit(3).map(this::toContentArticle).collect(Collectors.toList());
        sections.add(NewsletterContent.Section.builder()
                .title("🔥 오늘의 주요 뉴스")
                .description("가장 중요한 뉴스를 먼저 확인하세요")
                .articles(topNews)
                .sectionType("PERSONALIZED")
                .build());
        
        // 2. 카테고리별 섹션
        Map<String, List<NewsResponse>> categoryGroups = news.stream()
                .skip(3)
                .collect(Collectors.groupingBy(NewsResponse::getCategory));
        
        for (Map.Entry<String, List<NewsResponse>> entry : categoryGroups.entrySet()) {
            String categoryIcon = getCategoryIcon(entry.getKey());
            sections.add(NewsletterContent.Section.builder()
                    .title(categoryIcon + " " + entry.getKey())
                    .description(entry.getKey() + " 분야의 최신 소식")
                    .articles(convertToNewsletterArticles(entry.getValue()))
                        .sectionType("CATEGORY")
                    .build());
        }
        
        return sections;
    }

    private List<String> suggestOtherNewsletters(UserPreferenceProfile profile, String currentType) {
        List<String> suggestions = new ArrayList<>();
        
        // 현재 구독 중이지 않은 관련 뉴스레터 추천
        if (!currentType.equals("MORNING_BRIEFING") && profile.getMostActiveHour() < 10) {
            suggestions.add("MORNING_BRIEFING");
        }
        
        if (!currentType.equals("TECH_INSIGHTS") && 
            profile.getPreferredCategories().contains("IT/과학")) {
            suggestions.add("TECH_INSIGHTS");
        }
        
        if (!currentType.equals("WEEKLY_HIGHLIGHTS") && profile.getEngagementScore() > 0.7) {
            suggestions.add("WEEKLY_HIGHLIGHTS");
        }
        
        return suggestions;
    }

    // ========================================
    // Helper Methods - 분석 관련
    // ========================================

    private Map<Integer, Double> analyzeHourlyOpenRates(List<NewsletterDelivery> deliveries) {
        Map<Integer, List<NewsletterDelivery>> hourlyGroups = deliveries.stream()
                .filter(d -> d.getOpenedAt() != null)
                .collect(Collectors.groupingBy(d -> d.getOpenedAt().getHour()));
        
        Map<Integer, Double> hourlyRates = new HashMap<>();
        for (Map.Entry<Integer, List<NewsletterDelivery>> entry : hourlyGroups.entrySet()) {
            int hour = entry.getKey();
            long opened = entry.getValue().size();
            long total = deliveries.stream()
                    .filter(d -> d.getCreatedAt().getHour() == hour)
                    .count();
            
            double rate = total > 0 ? (double) opened / total * 100 : 0;
            hourlyRates.put(hour, rate);
        }
        
        return hourlyRates;
    }

    private Map<String, Double> analyzeCategoryEngagement(List<NewsletterDelivery> deliveries) {
        // 실제로는 뉴스레터 콘텐츠의 카테고리별 클릭률을 분석해야 함
        Map<String, Double> engagement = new HashMap<>();
        engagement.put("POLITICS", 4.2);
        engagement.put("ECONOMY", 4.7);
        engagement.put("TECH", 4.9);
        engagement.put("SOCIETY", 4.1);
        return engagement;
    }

    private List<SubscriptionTrendPoint> analyzeSubscriptionTrend(String newsletterType, int days) {
        List<SubscriptionTrendPoint> trend = new ArrayList<>();
        
        for (int i = days; i >= 0; i--) {
            LocalDateTime date = LocalDateTime.now().minusDays(i);
            long count = subscriptionRepository.countByPreferredCategoriesContainingAndCreatedAtBefore(
                    newsletterType, date);
            
            trend.add(SubscriptionTrendPoint.builder()
                    .date(date.toLocalDate())
                    .subscriberCount(count)
                    .dailyChange(i == days ? 0 : (count - trend.get(trend.size() - 1).getSubscriberCount()))
                    .build());
        }
        
        return trend;
    }

    private CompetitorAnalysis compareWithCompetitors(String newsletterType, double myOpenRate) {
        // 업계 평균과 비교 (실제로는 외부 데이터 소스에서 가져와야 함)
        Map<String, Double> industryBenchmarks = Map.of(
                "MORNING_BRIEFING", 25.3,
                "WEEKLY_HIGHLIGHTS", 28.7,
                "TECH_INSIGHTS", 22.1,
                "ECONOMY_WEEKLY", 31.2
        );
        
        double industryAverage = industryBenchmarks.getOrDefault(newsletterType, 24.5);
        double percentile = calculatePercentile(myOpenRate, industryAverage);
        
        return CompetitorAnalysis.builder()
                .myOpenRate(myOpenRate)
                .industryAverage(industryAverage)
                .percentileRank(percentile)
                .performanceLevel(determinePerformanceLevel(percentile))
                .recommendation(generateCompetitorRecommendation(percentile))
                .build();
    }

    private List<String> generatePerformanceRecommendations(double openRate, 
                                                          Map<String, Double> categoryEngagement) {
        List<String> recommendations = new ArrayList<>();
        
        if (openRate < 20) {
            recommendations.add("제목을 더 매력적으로 만들어 오픈률을 높여보세요");
            recommendations.add("발송 시간을 구독자들이 가장 활발한 시간대로 조정해보세요");
        }
        
        if (openRate > 30) {
            recommendations.add("훌륭한 성과입니다! 현재 전략을 유지하세요");
        }
        
        // 카테고리별 성과에 따른 추천
        String bestCategory = categoryEngagement.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
        
        if (!bestCategory.isEmpty()) {
            recommendations.add(bestCategory + " 카테고리의 콘텐츠를 더 늘려보세요");
        }
        
        return recommendations;
    }

    // ========================================
    // Helper Methods - 자동화 관련
    // ========================================

    private List<String> getAllNewsletterTypes() {
        return Arrays.asList(
                "MORNING_BRIEFING", "EVENING_REPORT", "WEEKLY_HIGHLIGHTS",
                "ECONOMY_WEEKLY", "TECH_INSIGHTS", "VIRAL_NEWS", "GLOBAL_TRENDS",
                "POLITICS", "ECONOMY", "SOCIETY", "CULTURE", "IT_SCIENCE"
        );
    }

    private List<Long> getActiveSubscribersByType(String newsletterType) {
        return subscriptionRepository.findByPreferredCategoriesContainingAndStatus(newsletterType, SubscriptionStatus.ACTIVE)
                .stream()
                .map(Subscription::getUserId)
                .collect(Collectors.toList());
    }

    private AggregatedPreferenceProfile analyzeAggregatedPreferences(List<Long> userIds) {
        // 여러 사용자의 선호도를 집계 분석
        Map<String, Integer> categoryFrequency = new HashMap<>();
        int totalUsers = userIds.size();
        
        for (Long userId : userIds) {
            List<CategoryResponse> preferences = getUserPreferences(userId);
            for (CategoryResponse pref : preferences) {
                categoryFrequency.merge(pref.getName(), 1, Integer::sum);
            }
        }
        
        return AggregatedPreferenceProfile.builder()
                .totalUsers(totalUsers)
                .categoryFrequency(categoryFrequency)
                .dominantCategories(findDominantCategories(categoryFrequency, totalUsers))
                .diversityScore(calculateDiversityScore(categoryFrequency))
                .analyzedAt(LocalDateTime.now())
                .build();
    }

    private List<NewsResponse> fetchRelevantNews(String newsletterType, AggregatedPreferenceProfile profile) {
        List<NewsResponse> relevantNews = new ArrayList<>();
        
        // 주요 카테고리별로 뉴스 수집
        for (String category : profile.getDominantCategories()) {
            try {
                List<NewsResponse> categoryNews = fetchNewsByCategory(category, 0, 10);
                relevantNews.addAll(categoryNews);
            } catch (Exception e) {
                log.warn("카테고리 {} 뉴스 수집 실패", category);
            }
        }
        
        return relevantNews;
    }

    private List<CuratedNewsItem> scoreAndRankNews(List<NewsResponse> news, AggregatedPreferenceProfile profile) {
        return news.stream()
                .map(newsItem -> {
                    double relevanceScore = calculateRelevanceScore(newsItem, profile);
                    double freshnessScore = calculateFreshnessScore(newsItem);
                    double qualityScore = calculateQualityScore(newsItem);
                    double totalScore = (relevanceScore * 0.4) + (freshnessScore * 0.3) + (qualityScore * 0.3);
                    
                    return CuratedNewsItem.builder()
                            .newsItem(newsItem)
                            .relevanceScore(relevanceScore)
                            .freshnessScore(freshnessScore)
                            .qualityScore(qualityScore)
                            .totalScore(totalScore)
                            .curatedAt(LocalDateTime.now())
                            .build();
                })
                .sorted((a, b) -> Double.compare(b.getTotalScore(), a.getTotalScore()))
                .limit(20)
                .collect(Collectors.toList());
    }

    private void saveCuratedContent(String newsletterType, List<CuratedNewsItem> items) {
        // 실제로는 Redis나 별도 캐시에 저장
        log.info("큐레이션 결과 저장: type={}, items={}", newsletterType, items.size());
    }

    // ========================================
    // Validation Methods
    // ========================================

    /**
     * 구독 요청 검증
     */
    private void validateSubscriptionRequest(SubscriptionRequest request, String userId) {
        // 기본 검증
        if (request.getEmail() == null || request.getEmail().trim().isEmpty()) {
            throw new NewsletterException("이메일은 필수입니다.", "INVALID_EMAIL");
        }
        
        if (request.getPreferredCategories() == null || request.getPreferredCategories().isEmpty()) {
            throw new NewsletterException("최소 1개의 카테고리를 선택해야 합니다.", "NO_CATEGORIES");
        }
        
        // 카테고리 개수 제한 검증
        Long userIdLong = Long.valueOf(userId);
        List<Subscription> existingSubscriptions = subscriptionRepository.findByUserIdAndStatus(userIdLong, SubscriptionStatus.ACTIVE);
        
        // 기존 구독의 카테고리 수 계산
        Set<NewsCategory> existingCategories = new HashSet<>();
        for (Subscription sub : existingSubscriptions) {
            if (sub.getPreferredCategories() != null) {
                List<NewsCategory> categories = parseJsonToCategories(sub.getPreferredCategories());
                existingCategories.addAll(categories);
            }
        }
        
        // 새로운 카테고리 추가 시 제한 확인
        Set<NewsCategory> newCategories = new HashSet<>(request.getPreferredCategories());
        newCategories.removeAll(existingCategories); // 기존에 없는 새로운 카테고리만
        
        int totalCategories = existingCategories.size() + newCategories.size();
        
        if (totalCategories > MAX_CATEGORIES_PER_USER) {
            throw new NewsletterException(
                String.format("최대 %d개 카테고리까지 구독할 수 있습니다. 현재 %d개 구독 중입니다.", 
                    MAX_CATEGORIES_PER_USER, existingCategories.size()),
                "CATEGORY_LIMIT_EXCEEDED"
            );
        }
        
        log.info("구독 요청 검증 완료: userId={}, existingCategories={}, newCategories={}, total={}", 
                userId, existingCategories.size(), newCategories.size(), totalCategories);
    }



    // ========================================
    // Utility Methods
    // ========================================

    private List<ReadHistoryResponse> getRecentUserInteractions(Long userId, int days) {
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(days);
            return userReadHistoryService.getReadHistory(userId, PageRequest.of(0, 100)).getContent();
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private List<NewsResponse> fetchNewsByCategory(String category, int page, int size) {
        try {
            ApiResponse<List<NewsResponse>> response = newsServiceClient.getNewsByCategory(category, page, size);
            return response != null && response.getData() != null ? response.getData() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("카테고리 뉴스 조회 실패: category={}", category);
            return new ArrayList<>();
        }
    }

    private String convertCategoriesToJson(List<CategoryResponse> categories) {
        if (categories == null || categories.isEmpty()) return "[]";
        
        try {
            List<String> categoryNames = categories.stream()
                    .map(CategoryResponse::getName)
                    .collect(Collectors.toList());
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(categoryNames);
        } catch (Exception e) {
            log.error("카테고리 JSON 변환 실패", e);
            return "[]";
        }
    }
    
    private String convertNewsCategoriesToJson(List<NewsCategory> categories) {
        if (categories == null || categories.isEmpty()) return "[]";
        
        try {
            List<String> categoryNames = categories.stream()
                    .map(NewsCategory::name)  // enum의 name() 사용
                    .collect(Collectors.toList());
            String json = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(categoryNames);
            log.debug("카테고리 JSON 변환: categories={}, categoryNames={}, json={}", categories, categoryNames, json);
            return json;
        } catch (Exception e) {
            log.error("뉴스 카테고리 JSON 변환 실패", e);
            return "[]";
        }
    }

    private String getCategoryIcon(String category) {
        return switch (category.toUpperCase()) {
            case "POLITICS" -> "🏛️";
            case "ECONOMY" -> "💰";
            case "SOCIETY" -> "👥";
            case "CULTURE" -> "🎭";
            case "INTERNATIONAL" -> "🌍";
            case "IT_SCIENCE" -> "💻";
            case "VEHICLE" -> "🚗";
            case "TRAVEL_FOOD" -> "✈️";
            case "ART" -> "🎨";
            default -> "📰";
        };
    }

    private double calculatePersonalizationScore(UserPreferenceProfile profile, List<NewsResponse> news) {
        if (profile.getPreferredCategories().isEmpty() || news.isEmpty()) return 0.5;
        
        long matchingNews = news.stream()
                .mapToLong(n -> profile.getPreferredCategories().contains(n.getCategory()) ? 1 : 0)
                .sum();
        
        return (double) matchingNews / news.size();
    }

    private int calculateReadTime(List<NewsletterContent.Section> sections) {
        return sections.stream()
                .mapToInt(section -> section.getArticles().size() * 2) // 기사당 2분 추정
                .sum();
    }

    private LocalDateTime calculateNextDelivery(Long userId, String newsletterType) {
        // 구독 설정에 따른 다음 발송 시간 계산
        Optional<Subscription> subscription = subscriptionRepository.findByUserId(userId);
        if (subscription.isPresent()) {
            SubscriptionFrequency frequency = subscription.get().getFrequency();
            int sendTime = subscription.get().getSendTime();
            
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime nextDelivery = now.withHour(sendTime).withMinute(0).withSecond(0);
            
            return switch (frequency) {
                case DAILY -> nextDelivery.isBefore(now) ? nextDelivery.plusDays(1) : nextDelivery;
                case WEEKLY -> nextDelivery.plusWeeks(1);
                case MONTHLY -> nextDelivery.plusMonths(1);
                case IMMEDIATE -> now; // 즉시 발송
            };
        }
        
        return LocalDateTime.now().plusDays(1).withHour(9).withMinute(0);
    }

    // 추가 helper methods...
    private double calculateClickThroughRate(List<NewsletterDelivery> deliveries) {
        // 실제로는 클릭 추적 데이터가 필요
        return 3.2 + (Math.random() * 2.0); // 임시 값
    }

    private double calculateUnsubscribeRate(String newsletterType, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        long unsubscribed = subscriptionRepository.countByPreferredCategoriesContainingAndStatusAndUpdatedAtAfter(
                newsletterType, SubscriptionStatus.UNSUBSCRIBED, since);
        long total = subscriptionRepository.countByPreferredCategoriesContaining(newsletterType);
        
        return total > 0 ? (double) unsubscribed / total * 100 : 0;
    }

    private double calculatePercentile(double myRate, double industryAverage) {
        return (myRate / industryAverage) * 50 + 25; // 간단한 계산
    }

    private String determinePerformanceLevel(double percentile) {
        if (percentile >= 80) return "EXCELLENT";
        if (percentile >= 60) return "GOOD";
        if (percentile >= 40) return "AVERAGE";
        return "NEEDS_IMPROVEMENT";
    }

    private String generateCompetitorRecommendation(double percentile) {
        if (percentile >= 80) return "업계 상위 20% 성과! 현재 전략을 유지하세요.";
        if (percentile >= 60) return "양호한 성과입니다. 콘텐츠 품질 개선으로 더 높은 성과를 노려보세요.";
        if (percentile >= 40) return "평균적인 성과입니다. 제목 개선과 발송 시간 최적화를 권장합니다.";
        return "성과 개선이 필요합니다. 구독자 선호도를 재분석하고 콘텐츠 전략을 재검토하세요.";
    }

    private List<String> findDominantCategories(Map<String, Integer> frequency, int totalUsers) {
        return frequency.entrySet().stream()
                .filter(entry -> entry.getValue() > totalUsers * 0.3) // 30% 이상 선호
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .map(Map.Entry::getKey)
                .limit(5)
                .collect(Collectors.toList());
    }

    private double calculateDiversityScore(Map<String, Integer> frequency) {
        if (frequency.isEmpty()) return 0;
        
        int totalPreferences = frequency.values().stream().mapToInt(Integer::intValue).sum();
        double entropy = frequency.values().stream()
                .mapToDouble(count -> {
                    double p = (double) count / totalPreferences;
                    return p * Math.log(p) / Math.log(2);
                })
                .sum();
        
        return -entropy; // Higher is more diverse
    }

    private double calculateRelevanceScore(NewsResponse news, AggregatedPreferenceProfile profile) {
        String category = news.getCategory();
        int frequency = profile.getCategoryFrequency().getOrDefault(category, 0);
        return (double) frequency / profile.getTotalUsers();
    }

    private double calculateFreshnessScore(NewsResponse news) {
        long hoursOld = ChronoUnit.HOURS.between(news.getPublishedAt(), LocalDateTime.now());
        return Math.max(0, 1.0 - (hoursOld / 24.0)); // 24시간 이내가 최고 점수
    }

    private double calculateQualityScore(NewsResponse news) {
        // 기사 품질을 평가하는 로직 (제목 길이, 요약 품질 등)
        double titleScore = Math.min(1.0, news.getTitle().length() / 100.0);
        double summaryScore = news.getSummary() != null ? 
                Math.min(1.0, news.getSummary().length() / 200.0) : 0.5;
        
        return (titleScore + summaryScore) / 2.0;
    }

    private Map<Integer, Integer> analyzeHourlyActivity(List<ReadHistoryResponse> readHistory) {
        return readHistory.stream()
                .collect(Collectors.groupingBy(
                        history -> history.getUpdatedAt().getHour(),
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
    }

    private int findMostActiveHour(Map<Integer, Integer> hourlyActivity) {
        return hourlyActivity.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(9); // 기본값
    }

    private int analyzePreferredReadTime(List<ReadHistoryResponse> interactions) {
        // 사용자의 선호 읽기 시간 분석 (간단히 평균값으로 계산)
        return 5; // 기본 5분
    }

    private double calculateUserEngagementScore(List<ReadHistoryResponse> interactions) {
        if (interactions.isEmpty()) return 0.0;
        
        // 읽기 기록 기반으로 참여도 계산
        return Math.min(1.0, interactions.size() / 30.0); // 30일 기준으로 정규화
    }

    private List<NewsletterContent.Article> convertToNewsletterArticles(List<NewsResponse> news) {
        return news.stream()
                .map(n -> NewsletterContent.Article.builder()
                        .id(n.getId())
                        .title(n.getTitle())
                        .summary(n.getSummary())
                        .category(n.getCategory())
                        .url(n.getSourceUrl())
                        .imageUrl(n.getImageUrl())
                        .publishedAt(n.getPublishedAt())
                        .viewCount(0L) // TODO: 실제 조회수 연동
                        .build())
                .collect(Collectors.toList());
    }

    private String generatePersonalizedTitle(String newsletterType, UserPreferenceProfile profile) {
        return getNewsletterDisplayName(newsletterType) + " - " + 
               profile.getPreferredCategories().stream().limit(2).collect(Collectors.joining(", ")) + 
               " 뉴스";
    }

    private String generatePersonalizedSubtitle(UserPreferenceProfile profile) {
        return "당신이 관심 있는 " + profile.getPreferredCategories().size() + 
               "개 카테고리의 최신 소식을 전해드립니다";
    }

    /**
     * 구독 재활성화
     */
    public SubscriptionResponse reactivateSubscription(Long subscriptionId, Long userId) {
        log.info("구독 재활성화: subscriptionId={}, userId={}", subscriptionId, userId);
        
        try {
            Subscription subscription = getSubscriptionWithPermissionCheck(subscriptionId, userId);
            
            if (subscription.getStatus() != SubscriptionStatus.UNSUBSCRIBED) {
                throw new NewsletterException("해지된 구독만 재활성화할 수 있습니다.", "INVALID_STATUS");
            }
            
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setUnsubscribedAt(null);
            subscription.setUpdatedAt(LocalDateTime.now());
            subscription = subscriptionRepository.save(subscription);
            
            log.info("구독 재활성화 완료: subscriptionId={}", subscriptionId);
            return convertToSubscriptionResponse(subscription);
            
        } catch (NewsletterException e) {
            throw e;
        } catch (Exception e) {
            log.error("구독 재활성화 중 오류 발생", e);
            throw new NewsletterException("구독 재활성화 중 오류가 발생했습니다.", "REACTIVATE_ERROR");
        }
    }

    // ========================================
    // 카테고리별 기사 조회 기능
    // ========================================

    /**
     * 카테고리별 기사 조회
     */
    public List<NewsResponse> getCategoryArticles(String category, int limit) {
        log.info("카테고리별 기사 조회: category={}, limit={}", category, limit);
        
        try {
            ApiResponse<List<NewsResponse>> response = newsServiceClient.getNewsByCategory(category, 0, limit);
            if (response != null && response.getData() != null) {
                return response.getData();
            }
            return new ArrayList<>();
        } catch (Exception e) {
            log.error("카테고리별 기사 조회 실패: category={}", category, e);
            return new ArrayList<>();
        }
    }

    /**
     * 카테고리별 기사와 트렌드 키워드 조회
     */
    public Map<String, Object> getCategoryArticlesWithTrendingKeywords(String category, int limit) {
        log.info("카테고리별 기사와 트렌드 키워드 조회: category={}, limit={}", category, limit);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 해당 카테고리의 최신 기사 조회
            List<NewsResponse> articles = getCategoryArticles(category, limit);
            result.put("articles", articles);
            
            // 2. 트렌드 키워드 조회
            List<String> trendingKeywords = getTrendingKeywordsByCategory(category, 8);
            result.put("trendingKeywords", trendingKeywords);
            result.put("mainTopics", trendingKeywords); // 트렌드 키워드를 주요 주제로 사용
            
            // 3. 총 기사 수 조회
            try {
                ApiResponse<Long> countResponse = newsServiceClient.getNewsCountByCategory(category);
                Long totalArticles = countResponse != null && countResponse.getData() != null ? 
                        countResponse.getData() : 0L;
                result.put("totalArticles", totalArticles);
            } catch (Exception e) {
                log.warn("카테고리별 기사 수 조회 실패: category={}", category, e);
                result.put("totalArticles", articles.size());
            }
            
            log.info("카테고리별 데이터 조회 완료: category={}, articles={}, keywords={}", 
                    category, articles.size(), trendingKeywords.size());
            
        } catch (Exception e) {
            log.error("카테고리별 기사와 트렌드 키워드 조회 실패: category={}", category, e);
            result.put("articles", new ArrayList<>());
            result.put("trendingKeywords", getDefaultKeywords());
            result.put("mainTopics", getDefaultKeywords());
            result.put("totalArticles", 0L);
        }
        
        return result;
    }

    /**
     * 카테고리별 트렌드 키워드 조회
     */
    public List<String> getTrendingKeywordsByCategory(String category, int limit) {
        log.info("카테고리별 트렌드 키워드 조회: category={}, limit={}", category, limit);
        
        try {
            ApiResponse<List<TrendingKeywordDto>> response = newsServiceClient.getTrendingKeywordsByCategory(category, limit, 24);
            if (response != null && response.getData() != null) {
                return response.getData().stream()
                        .map(TrendingKeywordDto::getKeyword)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("카테고리별 트렌드 키워드 조회 실패: category={}", category, e);
        }
        
        // 실패 시 기본 키워드 반환
        return getDefaultKeywords();
    }

    /**
     * 전체 트렌드 키워드 조회
     */
    public List<String> getTrendingKeywords(int limit) {
        log.info("전체 트렌드 키워드 조회: limit={}", limit);
        
        try {
            ApiResponse<List<TrendingKeywordDto>> response = newsServiceClient.getTrendingKeywords(limit, 24);
            if (response != null && response.getData() != null) {
                return response.getData().stream()
                        .map(TrendingKeywordDto::getKeyword)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("전체 트렌드 키워드 조회 실패", e);
        }
        
        // 실패 시 기본 키워드 반환
        return getDefaultKeywords();
    }

    /**
     * 기사에서 트렌드 키워드 추출
     */
    public List<String> extractTrendingKeywords(List<NewsResponse> articles) {
        if (articles == null || articles.isEmpty()) {
            return getDefaultKeywords();
        }

        // 기사 제목과 요약에서 키워드 추출
        Set<String> keywords = new HashSet<>();
        
        for (NewsResponse article : articles) {
            if (article.getTitle() != null) {
                keywords.addAll(extractKeywordsFromText(article.getTitle()));
            }
            if (article.getSummary() != null) {
                keywords.addAll(extractKeywordsFromText(article.getSummary()));
            }
        }

        // 키워드 빈도 계산 및 정렬
        Map<String, Integer> keywordFrequency = new HashMap<>();
        for (String keyword : keywords) {
            keywordFrequency.merge(keyword, 1, Integer::sum);
        }

        return keywordFrequency.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(8)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 기사에서 주요 주제 추출
     */
    public List<String> extractMainTopics(List<NewsResponse> articles) {
        if (articles == null || articles.isEmpty()) {
            return getDefaultTopics();
        }

        // 기사 제목에서 주요 주제 추출
        Set<String> topics = new HashSet<>();
        
        for (NewsResponse article : articles) {
            if (article.getTitle() != null) {
                topics.addAll(extractTopicsFromTitle(article.getTitle()));
            }
        }

        return topics.stream()
                .limit(6)
                .collect(Collectors.toList());
    }

    /**
     * 텍스트에서 키워드 추출
     */
    private Set<String> extractKeywordsFromText(String text) {
        Set<String> keywords = new HashSet<>();
        
        // 간단한 키워드 추출 로직 (실제로는 NLP 라이브러리 사용 권장)
        String[] words = text.split("\\s+");
        for (String word : words) {
            // 2글자 이상의 한글 단어만 키워드로 추출
            if (word.length() >= 2 && word.matches(".*[가-힣].*")) {
                keywords.add(word);
            }
        }
        
        return keywords;
    }

    /**
     * 제목에서 주요 주제 추출
     */
    private Set<String> extractTopicsFromTitle(String title) {
        Set<String> topics = new HashSet<>();
        
        // 제목에서 주요 주제 추출 로직
        String[] words = title.split("\\s+");
        for (String word : words) {
            // 3글자 이상의 한글 단어만 주제로 추출
            if (word.length() >= 3 && word.matches(".*[가-힣].*")) {
                topics.add(word);
            }
        }
        
        return topics;
    }

    /**
     * 기본 키워드 반환
     */
    private List<String> getDefaultKeywords() {
        return Arrays.asList("주요뉴스", "핫이슈", "트렌드", "분석", "전망", "동향", "소식", "업데이트");
    }

    /**
     * 기본 주제 반환
     */
    private List<String> getDefaultTopics() {
        return Arrays.asList("주요뉴스", "핫이슈", "트렌드", "분석", "전망", "동향");
    }

}