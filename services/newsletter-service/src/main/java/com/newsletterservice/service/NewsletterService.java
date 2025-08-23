package com.newsletterservice.service;

import com.newsletterservice.client.NewsServiceClient;
import com.newsletterservice.client.UserServiceClient;
import com.newsletterservice.client.dto.CategoryResponse;
import com.newsletterservice.client.dto.NewsResponse;
import com.newsletterservice.client.dto.UserResponse;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.common.exception.NewsletterException;
import com.newsletterservice.dto.*;
import com.newsletterservice.entity.*;
import com.newsletterservice.repository.NewsletterDeliveryRepository;
import com.newsletterservice.repository.SubscriptionRepository;
import com.newsletterservice.repository.UserNewsInteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.Duration;
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
    private final UserNewsInteractionRepository interactionRepository;

    // ========================================
    // Client Dependencies
    // ========================================
    private final NewsServiceClient newsServiceClient;
    private final UserServiceClient userServiceClient;

    // ========================================
    // Service Dependencies
    // ========================================
    private final EmailNewsletterRenderer emailRenderer;

    // ========================================
    // Constants
    // ========================================
    private static final int MAX_ITEMS = 8;
    private static final int PER_CATEGORY_LIMIT = 3;

    // ========================================
    // 1. 구독 관리 기능
    // ========================================

    /**
     * 구독 생성
     */
    public SubscriptionResponse subscribe(SubscriptionRequest request, String userId) {
        log.info("구독 생성 요청: userId={}, categories={}", userId, request.getPreferredCategories());
        
        try {
            // 기존 구독 확인
            Optional<Subscription> existingSubscription = subscriptionRepository.findByUserId(Long.valueOf(userId));
            
            Subscription subscription;
            if (existingSubscription.isPresent()) {
                // 기존 구독 업데이트
                subscription = existingSubscription.get();
                subscription.setStatus(SubscriptionStatus.ACTIVE);
                subscription.setEmail(request.getEmail());
                subscription.setFrequency(request.getFrequency());
                subscription.setPreferredCategories(convertCategoriesToJson(request.getPreferredCategories()));
                subscription.setKeywords(convertToJson(request.getKeywords()));
                subscription.setSendTime(request.getSendTime() != null ? request.getSendTime() : 9);
                subscription.setPersonalized(request.isPersonalized());
                subscription.setUpdatedAt(LocalDateTime.now());
            } else {
                // 새 구독 생성
                subscription = Subscription.builder()
                        .userId(Long.valueOf(userId))
                        .email(request.getEmail())
                        .preferredCategories(convertCategoriesToJson(request.getPreferredCategories()))
                        .keywords(convertToJson(request.getKeywords()))
                        .frequency(request.getFrequency())
                        .sendTime(request.getSendTime() != null ? request.getSendTime() : 9)
                        .isPersonalized(request.isPersonalized())
                        .status(SubscriptionStatus.ACTIVE)
                        .subscribedAt(LocalDateTime.now())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();
            }
            
            Subscription savedSubscription = subscriptionRepository.save(subscription);
            log.info("구독 생성 완료: subscriptionId={}", savedSubscription.getId());
            
            return convertToSubscriptionResponse(savedSubscription, request);
            
        } catch (Exception e) {
            log.error("구독 생성 실패: userId={}", userId, e);
            throw new NewsletterException("구독 생성 중 오류가 발생했습니다.", "SUBSCRIPTION_ERROR");
        }
    }

    /**
     * 구독 해지
     */
    public void unsubscribe(Long subscriptionId) {
        log.info("구독 해지 요청: subscriptionId={}", subscriptionId);
        
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new NewsletterException("구독 정보를 찾을 수 없습니다.", "SUBSCRIPTION_NOT_FOUND"));
        
        subscription.setStatus(SubscriptionStatus.UNSUBSCRIBED);
        subscription.setUnsubscribedAt(LocalDateTime.now());
        subscription.setUpdatedAt(LocalDateTime.now());
        
        subscriptionRepository.save(subscription);
        log.info("구독 해지 완료: subscriptionId={}", subscriptionId);
    }

    /**
     * 구독 정보 조회
     */
    public SubscriptionResponse getSubscription(Long subscriptionId, Long userId) {
        log.info("구독 정보 조회: subscriptionId={}, userId={}", subscriptionId, userId);
        
        try {
            Subscription subscription = subscriptionRepository.findById(subscriptionId)
                    .orElseThrow(() -> new NewsletterException("구독을 찾을 수 없습니다.", "SUBSCRIPTION_NOT_FOUND"));
            
            // 권한 확인
            if (!subscription.getUserId().equals(userId)) {
                throw new NewsletterException("구독 정보를 조회할 권한이 없습니다.", "UNAUTHORIZED");
            }
            
            return convertToSubscriptionResponse(subscription, null);
            
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
                    .map(subscription -> convertToSubscriptionResponse(subscription, null))
                    .collect(Collectors.toList());
            
        } catch (Exception e) {
            log.error("내 구독 목록 조회 중 오류 발생", e);
            throw new NewsletterException("구독 목록 조회 중 오류가 발생했습니다.", "SUBSCRIPTION_LIST_ERROR");
        }
    }

    /**
     * 구독 해지 (권한 확인 포함)
     */
    public void unsubscribe(Long subscriptionId, Long userId) {
        log.info("구독 해지: subscriptionId={}, userId={}", subscriptionId, userId);
        
        try {
            Subscription subscription = subscriptionRepository.findById(subscriptionId)
                    .orElseThrow(() -> new NewsletterException("구독을 찾을 수 없습니다.", "SUBSCRIPTION_NOT_FOUND"));
            
            // 권한 확인
            if (!subscription.getUserId().equals(userId)) {
                throw new NewsletterException("구독을 해지할 권한이 없습니다.", "UNAUTHORIZED");
            }
            
            subscription.setStatus(SubscriptionStatus.UNSUBSCRIBED);
            subscription.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            
        } catch (NewsletterException e) {
            throw e;
        } catch (Exception e) {
            log.error("구독 해지 중 오류 발생", e);
            throw new NewsletterException("구독 해지 중 오류가 발생했습니다.", "UNSUBSCRIBE_ERROR");
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
                    .map(subscription -> convertToSubscriptionResponse(subscription, null))
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
            Subscription subscription = subscriptionRepository.findById(subscriptionId)
                    .orElseThrow(() -> new NewsletterException("구독을 찾을 수 없습니다.", "SUBSCRIPTION_NOT_FOUND"));
            
            // 권한 확인
            if (!subscription.getUserId().equals(userId)) {
                throw new NewsletterException("구독 상태를 변경할 권한이 없습니다.", "UNAUTHORIZED");
            }
            
            // 상태값 검증
            SubscriptionStatus status;
            try {
                status = SubscriptionStatus.valueOf(newStatus.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new NewsletterException("유효하지 않은 구독 상태입니다.", "INVALID_STATUS");
            }
            
            subscription.setStatus(status);
            subscription.setUpdatedAt(LocalDateTime.now());
            subscription = subscriptionRepository.save(subscription);
            
            return convertToSubscriptionResponse(subscription, null);
            
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

    // ========================================
    // 2. 뉴스레터 콘텐츠 생성 기능
    // ========================================

    /**
     * 개인화된 뉴스레터 콘텐츠 생성
     */
    public NewsletterContent buildPersonalizedContent(Long userId, Long newsletterId) {
        log.info("개인화된 뉴스레터 콘텐츠 생성: userId={}, newsletterId={}", userId, newsletterId);
        
        // 1) 구독 여부 확인
        if (!isSubscribed(userId)) {
            throw new NewsletterException("활성 구독이 필요합니다.", "SUBSCRIPTION_REQUIRED");
        }

        // 2) 선호 카테고리 조회
        List<CategoryResponse> prefs = getUserPreferences(userId);
        boolean hasPrefs = prefs != null && !prefs.isEmpty();

        // 3) 카테고리별 수집
        List<NewsResponse> picked = new ArrayList<>();

        if (hasPrefs) {
            for (CategoryResponse category : prefs) {
                if (picked.size() >= MAX_ITEMS) break;
                
                try {
                    List<NewsResponse> byCat = fetchLatestByCategory(category.getName(), PER_CATEGORY_LIMIT);
                    appendDistinct(picked, byCat, MAX_ITEMS);
                    log.debug("카테고리 {}에서 {}개 뉴스 수집", category.getName(), byCat != null ? byCat.size() : 0);
                } catch (Exception e) {
                    log.warn("카테고리 {} 뉴스 조회 실패: {}", category.getName(), e.getMessage());
                }
            }
        }

        // 4) 잔여 슬롯은 글로벌 인기/최신으로 채우기
        if (picked.size() < MAX_ITEMS) {
            try {
                List<NewsResponse> popular = fetchPopular(MAX_ITEMS - picked.size());
                appendDistinct(picked, popular, MAX_ITEMS);
            } catch (Exception e) {
                log.warn("인기 뉴스 조회 실패: {}", e.getMessage());
            }
        }
        
        if (picked.size() < MAX_ITEMS) {
            try {
                List<NewsResponse> latest = fetchLatest(MAX_ITEMS - picked.size());
                appendDistinct(picked, latest, MAX_ITEMS);
            } catch (Exception e) {
                log.warn("최신 뉴스 조회 실패: {}", e.getMessage());
            }
        }

        // 5) NewsletterContent 조립
        NewsletterContent content = NewsletterContent.builder()
                .newsletterId(newsletterId)
                .userId(userId)
                .personalized(hasPrefs)
                .title(generateTitle(hasPrefs))
                .generatedAt(LocalDateTime.now())
                .sections(buildSections(picked, hasPrefs))
                .build();

        log.info("개인화된 뉴스레터 콘텐츠 생성 완료: 총 {}개 뉴스", picked.size());
        return content;
    }

    // ========================================
    // 3. 뉴스레터 발송 기능
    // ========================================

    /**
     * 뉴스레터 발송 예약
     */
    public NewsletterDelivery scheduleDelivery(NewsletterDelivery requestDTO) {
        log.info("뉴스레터 발송 예약: userId={}, newsletterId={}", requestDTO.getUserId(), requestDTO.getNewsletterId());

        NewsletterDelivery delivery = NewsletterDelivery.builder()
                .newsletterId(requestDTO.getNewsletterId())
                .userId(requestDTO.getUserId())
                .status(DeliveryStatus.PENDING)
                .deliveryMethod(requestDTO.getDeliveryMethod())
                .scheduledAt(requestDTO.getScheduledAt())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        NewsletterDelivery saved = deliveryRepository.save(delivery);
        log.info("뉴스레터 발송 예약 완료: deliveryId={}", saved.getId());

        return saved;
    }

    /**
     * 즉시 발송
     */
    public NewsletterDelivery sendImmediately(NewsletterDelivery requestDTO) {
        log.info("즉시 발송 요청: userId={}, newsletterId={}", requestDTO.getUserId(), requestDTO.getNewsletterId());

        NewsletterDelivery delivery = NewsletterDelivery.builder()
                .newsletterId(requestDTO.getNewsletterId())
                .userId(requestDTO.getUserId())
                .status(DeliveryStatus.PENDING)
                .deliveryMethod(requestDTO.getDeliveryMethod())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        NewsletterDelivery saved = deliveryRepository.save(delivery);

        // 실제 발송 로직 수행
        performDelivery(saved);

        return saved;
    }

    /**
     * 발송 취소
     */
    public NewsletterDelivery cancelDelivery(Long deliveryId) {
        log.info("발송 취소 요청: deliveryId={}", deliveryId);

        NewsletterDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new NewsletterException("발송 정보를 찾을 수 없습니다.", "DELIVERY_NOT_FOUND"));

        if (delivery.getStatus() == DeliveryStatus.SENT) {
            throw new NewsletterException("이미 발송된 뉴스레터는 취소할 수 없습니다.", "ALREADY_SENT");
        }

        delivery.updateStatus(DeliveryStatus.BOUNCED);
        delivery.setUpdatedAt(LocalDateTime.now());
        NewsletterDelivery updated = deliveryRepository.save(delivery);

        log.info("발송 취소 완료: deliveryId={}", deliveryId);
        return updated;
    }

    // ========================================
    // 4. 스케줄링 기능
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
                .findByStatusAndUpdatedAtAfterAndRetryCountLessThan(DeliveryStatus.FAILED, cutoff, 3);

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
    // 5. 분석 기능
    // ========================================

    /**
     * 사용자 참여도 분석
     */
    @Transactional(readOnly = true)
    public UserEngagement analyzeUserEngagement(Long userId, int days) {
        log.info("사용자 참여도 분석: userId={}, days={}", userId, days);
        
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        Optional<Object[]> statsOpt = deliveryRepository.getUserEngagementStats(userId, since);
        
        if (statsOpt.isEmpty()) {
            return UserEngagement.builder()
                .userId(userId)
                .engagementRate(0.0)
                .recommendation("데이터가 부족합니다. 더 많은 뉴스레터를 받아보세요.")
                .build();
        }
        
        Object[] stats = statsOpt.get();
        long totalReceived = ((Number) stats[1]).longValue();
        long totalOpened = ((Number) stats[2]).longValue();
        Double avgOpenDelayMinutes = stats[3] != null ? ((Number) stats[3]).doubleValue() : null;
        
        double engagementRate = totalReceived > 0 ? (double) totalOpened / totalReceived * 100 : 0;
        
        String recommendation = generateEngagementRecommendation(engagementRate, avgOpenDelayMinutes, totalReceived);
        
        return UserEngagement.builder()
            .userId(userId)
            .totalReceived(totalReceived)
            .totalOpened(totalOpened)
            .engagementRate(engagementRate)
            .avgOpenDelayMinutes(avgOpenDelayMinutes)
            .recommendation(recommendation)
            .analysisPeriod(days)
            .build();
    }

    /**
     * 실시간 대시보드 통계
     */
    @Transactional(readOnly = true)
    public RealTimeStats getRealTimeStats(int hours) {
        log.info("실시간 통계 조회: hours={}", hours);
        
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        Object[] stats = deliveryRepository.getRealTimeStats(since);
        
        return RealTimeStats.builder()
            .pendingCount(((Number) stats[0]).longValue())
            .processingCount(((Number) stats[1]).longValue())
            .sentCount(((Number) stats[2]).longValue())
            .openedCount(((Number) stats[3]).longValue())
            .failedCount(((Number) stats[4]).longValue())
            .bouncedCount(((Number) stats[5]).longValue())
            .timestamp(LocalDateTime.now())
            .analysisPeriodHours(hours)
            .build();
    }

    // ========================================
    // 6. 사용자 행동 추적 기능
    // ========================================

    /**
     * 사용자 뉴스 조회 기록
     */
    public void trackNewsView(Long userId, Long newsId, String category) {
        try {
            UserNewsInteraction interaction = UserNewsInteraction.builder()
                    .userId(userId)
                    .newsId(newsId)
                    .category(category)
                    .type(InteractionType.VIEW)
                    .createdAt(LocalDateTime.now())
                    .build();

            interactionRepository.save(interaction);
            log.debug("뉴스 조회 기록: userId={}, newsId={}, category={}", userId, newsId, category);
        } catch (Exception e) {
            log.error("뉴스 조회 기록 실패", e);
        }
    }

    /**
     * 사용자 뉴스 클릭 기록
     */
    public void trackNewsClick(Long userId, Long newsId, String category) {
        try {
            UserNewsInteraction interaction = UserNewsInteraction.builder()
                    .userId(userId)
                    .newsId(newsId)
                    .category(category)
                    .type(InteractionType.CLICK)
                    .createdAt(LocalDateTime.now())
                    .build();

            interactionRepository.save(interaction);
            log.debug("뉴스 클릭 기록: userId={}, newsId={}, category={}", userId, newsId, category);
        } catch (Exception e) {
            log.error("뉴스 클릭 기록 실패", e);
        }
    }

    // ========================================
    // 7. 조회 기능
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

    // ========================================
    // Private Helper Methods
    // ========================================

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

    private List<NewsResponse> fetchLatestByCategory(String categoryName, int limit) {
        try {
            ApiResponse<List<NewsResponse>> response = newsServiceClient.getLatestByCategory(categoryName, limit);
            return response != null ? response.getData() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("카테고리 {} 최신 뉴스 조회 실패: {}", categoryName, e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<NewsResponse> fetchPopular(int limit) {
        try {
            ApiResponse<List<NewsResponse>> response = newsServiceClient.getPopularNews(limit);
            return response != null ? response.getData() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("인기 뉴스 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<NewsResponse> fetchLatest(int limit) {
        try {
            ApiResponse<List<NewsResponse>> response = newsServiceClient.getLatestNews(null, limit);
            return response != null ? response.getData() : new ArrayList<>();
        } catch (Exception e) {
            log.warn("최신 뉴스 조회 실패: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private void appendDistinct(List<NewsResponse> acc, List<NewsResponse> candidates, int max) {
        if (candidates == null || candidates.isEmpty()) return;
        
        Set<Long> existingIds = acc.stream()
                .map(NewsResponse::getId)
                .collect(Collectors.toSet());
        
        for (NewsResponse news : candidates) {
            if (acc.size() >= max) break;
            if (existingIds.add(news.getId())) {
                acc.add(news);
            }
        }
    }

    private String generateTitle(boolean isPersonalized) {
        return isPersonalized ? "당신을 위한 맞춤 뉴스레터" : "오늘의 핫한 뉴스";
    }

    private List<NewsletterContent.Section> buildSections(List<NewsResponse> newsList, boolean isPersonalized) {
        List<NewsletterContent.Section> sections = new ArrayList<>();
        
        if (newsList.isEmpty()) {
            sections.add(NewsletterContent.Section.builder()
                    .heading("오늘의 뉴스")
                    .sectionType("DEFAULT")
                    .description("현재 뉴스를 불러올 수 없습니다.")
                    .articles(new ArrayList<>())
                    .build());
        } else {
            List<NewsletterContent.Article> articles = newsList.stream()
                    .map(this::toContentArticle)
                    .collect(Collectors.toList());
            
            sections.add(NewsletterContent.Section.builder()
                    .heading(isPersonalized ? "당신을 위한 뉴스" : "오늘의 뉴스")
                    .sectionType(isPersonalized ? "PERSONALIZED" : "TRENDING")
                    .description(isPersonalized ? "관심 카테고리 기반으로 선별된 뉴스입니다." : "현재 인기 있는 뉴스입니다.")
                    .articles(articles)
                    .build());
        }
        
        return sections;
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
        // TODO: SMS 발송 로직
    }

    private void sendByPushNotification(NewsletterDelivery delivery) {
        log.info("푸시 알림 발송: userId={}", delivery.getUserId());
        // TODO: 푸시 알림 발송 로직
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

    private String generateEngagementRecommendation(double engagementRate, Double avgOpenDelay, long totalReceived) {
        if (engagementRate > 40) {
            return "매우 높은 참여도입니다! 개인화를 더욱 강화하거나 발송 빈도를 늘려보세요.";
        } else if (engagementRate > 25) {
            return "좋은 참여도입니다. 현재 설정을 유지하시면 됩니다.";
        } else if (engagementRate > 15) {
            return "참여도가 보통 수준입니다. 콘텐츠 품질을 개선하거나 발송 시간을 조정해보세요.";
        } else {
            return "참여도가 낮습니다. 구독 빈도를 줄이거나 관심 키워드를 재설정해보세요.";
        }
    }

    private SubscriptionResponse convertToSubscriptionResponse(Subscription subscription, SubscriptionRequest request) {
        return SubscriptionResponse.builder()
                .id(subscription.getId())
                .userId(subscription.getUserId())
                .email(subscription.getEmail())
                .preferredCategories(request != null ? request.getPreferredCategories() : null)
                .keywords(request != null ? request.getKeywords() : null)
                .frequency(subscription.getFrequency())
                .status(subscription.getStatus())
                .sendTime(subscription.getSendTime())
                .isPersonalized(subscription.isPersonalized())
                .subscribedAt(subscription.getSubscribedAt())
                .lastSentAt(subscription.getLastSentAt())
                .createdAt(subscription.getCreatedAt())
                .build();
    }

    private String convertCategoriesToJson(List<NewsCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return "[]";
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(categories);
        } catch (Exception e) {
            log.error("카테고리 JSON 변환 실패", e);
            return "[]";
        }
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

    // ========================================
    // 3. 발송 관리 기능
    // ========================================

    /**
     * 뉴스레터 즉시 발송
     */
    public DeliveryStats sendNewsletterNow(NewsletterDeliveryRequest request, Long senderId) {
        log.info("뉴스레터 즉시 발송 요청: newsletterId={}, targetUserIds={}, deliveryMethod={}", 
                request.getNewsletterId(), request.getTargetUserIds(), request.getDeliveryMethod());
        
        try {
            // 발송 통계 초기화
            int totalTargets = request.getTargetUserIds().size();
            int successCount = 0;
            int failureCount = 0;
            List<String> errors = new ArrayList<>();
            
            // 각 대상 사용자에게 발송
            for (Long targetUserId : request.getTargetUserIds()) {
                try {
                    // 개인화된 콘텐츠 생성
                    NewsletterContent content = buildPersonalizedContent(targetUserId, request.getNewsletterId());
                    
                    // 발송 기록 생성
                    NewsletterDelivery delivery = NewsletterDelivery.builder()
                            .userId(targetUserId)
                            .newsletterId(request.getNewsletterId())
                            .deliveryMethod(request.getDeliveryMethod())
                            .status(DeliveryStatus.PROCESSING)
                            .scheduledAt(LocalDateTime.now())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    
                    deliveryRepository.save(delivery);
                    
                    // 실제 발송 수행
                    performDelivery(delivery);
                    
                    successCount++;
                    log.info("뉴스레터 발송 성공: userId={}, newsletterId={}", targetUserId, request.getNewsletterId());
                    
                } catch (Exception e) {
                    failureCount++;
                    String errorMsg = String.format("사용자 %d 발송 실패: %s", targetUserId, e.getMessage());
                    errors.add(errorMsg);
                    log.error("뉴스레터 발송 실패: userId={}, newsletterId={}", targetUserId, request.getNewsletterId(), e);
                }
            }
            
            // 발송 통계 반환
            double successRate = totalTargets > 0 ? (double) successCount / totalTargets * 100 : 0.0;
            return DeliveryStats.builder()
                    .totalSent(successCount)
                    .totalFailed(failureCount)
                    .totalScheduled(totalTargets)
                    .successRate(successRate)
                    .build();
                    
        } catch (Exception e) {
            log.error("뉴스레터 즉시 발송 중 오류 발생", e);
            throw new NewsletterException("뉴스레터 발송 중 오류가 발생했습니다.", "DELIVERY_ERROR");
        }
    }

    /**
     * 뉴스레터 예약 발송
     */
    public DeliveryStats scheduleNewsletter(NewsletterDeliveryRequest request, Long senderId) {
        log.info("뉴스레터 예약 발송 요청: newsletterId={}, targetUserIds={}, deliveryMethod={}", 
                request.getNewsletterId(), request.getTargetUserIds(), request.getDeliveryMethod());
        
        try {
            // 발송 통계 초기화
            int totalTargets = request.getTargetUserIds().size();
            int scheduledCount = 0;
            int failureCount = 0;
            
            // 각 대상 사용자에게 예약 발송
            for (Long targetUserId : request.getTargetUserIds()) {
                try {
                    // 예약 발송 기록 생성
                    NewsletterDelivery delivery = NewsletterDelivery.builder()
                            .userId(targetUserId)
                            .newsletterId(request.getNewsletterId())
                            .deliveryMethod(request.getDeliveryMethod())
                            .status(DeliveryStatus.SCHEDULED)
                            .scheduledAt(request.getScheduledAt() != null ? request.getScheduledAt() : LocalDateTime.now().plusHours(1))
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    
                    deliveryRepository.save(delivery);
                    
                    scheduledCount++;
                    log.info("뉴스레터 예약 성공: userId={}, newsletterId={}, scheduledAt={}", 
                            targetUserId, request.getNewsletterId(), delivery.getScheduledAt());
                    
                } catch (Exception e) {
                    failureCount++;
                    log.error("뉴스레터 예약 실패: userId={}, newsletterId={}", targetUserId, request.getNewsletterId(), e);
                }
            }
            
            // 발송 통계 반환
            double successRate = totalTargets > 0 ? (double) scheduledCount / totalTargets * 100 : 0.0;
            return DeliveryStats.builder()
                    .totalSent(scheduledCount)
                    .totalFailed(failureCount)
                    .totalScheduled(totalTargets)
                    .successRate(successRate)
                    .build();
                    
        } catch (Exception e) {
            log.error("뉴스레터 예약 발송 중 오류 발생", e);
            throw new NewsletterException("뉴스레터 예약 중 오류가 발생했습니다.", "SCHEDULE_ERROR");
        }
    }

    /**
     * 발송 취소
     */
    public void cancelDelivery(Long deliveryId, Long userId) {
        log.info("발송 취소 요청: deliveryId={}, userId={}", deliveryId, userId);
        
        try {
            NewsletterDelivery delivery = deliveryRepository.findById(deliveryId)
                    .orElseThrow(() -> new NewsletterException("발송 기록을 찾을 수 없습니다.", "DELIVERY_NOT_FOUND"));
            
            // 권한 확인 (발송자 또는 수신자만 취소 가능)
            if (!delivery.getUserId().equals(userId)) {
                throw new NewsletterException("발송을 취소할 권한이 없습니다.", "UNAUTHORIZED");
            }
            
            // 취소 가능한 상태인지 확인
            if (delivery.getStatus() == DeliveryStatus.SENT || delivery.getStatus() == DeliveryStatus.FAILED) {
                throw new NewsletterException("이미 처리된 발송은 취소할 수 없습니다.", "INVALID_STATUS");
            }
            
            delivery.updateStatus(DeliveryStatus.CANCELLED);
            delivery.setUpdatedAt(LocalDateTime.now());
            deliveryRepository.save(delivery);
            
            log.info("발송 취소 완료: deliveryId={}", deliveryId);
            
        } catch (NewsletterException e) {
            throw e;
        } catch (Exception e) {
            log.error("발송 취소 중 오류 발생: deliveryId={}", deliveryId, e);
            throw new NewsletterException("발송 취소 중 오류가 발생했습니다.", "CANCEL_ERROR");
        }
    }

    /**
     * 발송 재시도
     */
    public void retryDelivery(Long deliveryId, Long userId) {
        log.info("발송 재시도 요청: deliveryId={}, userId={}", deliveryId, userId);
        
        try {
            NewsletterDelivery delivery = deliveryRepository.findById(deliveryId)
                    .orElseThrow(() -> new NewsletterException("발송 기록을 찾을 수 없습니다.", "DELIVERY_NOT_FOUND"));
            
            // 권한 확인
            if (!delivery.getUserId().equals(userId)) {
                throw new NewsletterException("발송을 재시도할 권한이 없습니다.", "UNAUTHORIZED");
            }
            
            // 재시도 가능한 상태인지 확인
            if (delivery.getStatus() != DeliveryStatus.FAILED) {
                throw new NewsletterException("실패한 발송만 재시도할 수 있습니다.", "INVALID_STATUS");
            }
            
            // 재시도 횟수 제한 확인
            if (delivery.getRetryCount() >= 3) {
                throw new NewsletterException("최대 재시도 횟수를 초과했습니다.", "MAX_RETRY_EXCEEDED");
            }
            
            retryFailedDelivery(delivery);
            
            log.info("발송 재시도 완료: deliveryId={}", deliveryId);
            
        } catch (NewsletterException e) {
            throw e;
        } catch (Exception e) {
            log.error("발송 재시도 중 오류 발생: deliveryId={}", deliveryId, e);
            throw new NewsletterException("발송 재시도 중 오류가 발생했습니다.", "RETRY_ERROR");
        }
    }
}
