package com.newsletterservice.controller;

import com.newsletterservice.client.dto.UserResponse;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.dto.NewsletterCreateRequest;
import com.newsletterservice.dto.DeliveryStats;
import com.newsletterservice.entity.NewsletterDelivery;
import com.newsletterservice.entity.DeliveryStatus;
import com.newsletterservice.entity.NewsCategory;
import com.newsletterservice.service.NewsletterDeliveryService;
import com.newsletterservice.service.ContentGenerationService;
import com.newsletterservice.client.dto.NewsResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/newsletter-delivery")
@RequiredArgsConstructor
public class NewsletterDeliveryController {

    private final NewsletterDeliveryService deliveryService;
    private final ContentGenerationService contentGenerationService;

    /**
     * 뉴스레터 최신 뉴스 조회
     */
    @GetMapping("/content/latest")
    public ResponseEntity<ApiResponse<List<NewsResponse>>> getLatestNewsContent(
            @RequestParam(required = false) Long newsletterId) {
        
        log.info("Fetching latest news content for newsletterId: {}", newsletterId);
        
        try {
            NewsletterCreateRequest request = NewsletterCreateRequest.builder()
                    .newsletterId(newsletterId != null ? newsletterId : 1L)
                    .build();
            
            List<NewsResponse> newsData = contentGenerationService.getLatestNewsData(request);
            
            return ResponseEntity.ok(
                    ApiResponse.success(newsData, "Latest news data retrieved successfully."));
                    
        } catch (Exception e) {
            log.error("Failed to fetch latest news content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("CONTENT_FETCH_ERROR", "Failed to fetch latest news content: " + e.getMessage()));
        }
    }

    /**
     * 개인화된 뉴스 데이터 조회
     */
    @GetMapping("/content/personalized")
    public ResponseEntity<ApiResponse<List<NewsResponse>>> getPersonalizedNewsContent(
            @RequestParam(required = false) Long newsletterId,
            @RequestParam(required = false) List<Long> userIds) {
        
        log.info("Fetching personalized news content for newsletterId: {}, userIds: {}", newsletterId, userIds);
        
        try {
            NewsletterCreateRequest request = NewsletterCreateRequest.builder()
                    .newsletterId(newsletterId != null ? newsletterId : 1L)
                    .isPersonalized(true)
                    .build();
            
            List<NewsResponse> newsData = contentGenerationService.getPersonalizedNewsData(request, userIds);
            
            return ResponseEntity.ok(
                    ApiResponse.success(newsData, "Personalized news data retrieved successfully."));
                    
        } catch (Exception e) {
            log.error("Failed to fetch personalized news content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("CONTENT_FETCH_ERROR", "Failed to fetch personalized news content: " + e.getMessage()));
        }
    }

    /**
     * 카테고리별 뉴스 데이터 조회
     */
    @GetMapping("/content/categories")
    public ResponseEntity<ApiResponse<List<NewsResponse>>> getCategoryNewsContent(
            @RequestParam(required = false) Long newsletterId,
            @RequestParam(required = false) Set<NewsCategory> categories) {
        
        log.info("Fetching category news content for newsletterId: {}, categories: {}", newsletterId, categories);
        
        try {
            NewsletterCreateRequest request = NewsletterCreateRequest.builder()
                    .newsletterId(newsletterId != null ? newsletterId : 1L)
                    .build();
            
            Set<NewsCategory> targetCategories = categories != null ? categories : 
                    Set.of(NewsCategory.POLITICS, NewsCategory.ECONOMY, NewsCategory.SOCIETY);
            
            List<NewsResponse> newsData = contentGenerationService.getCategoryNewsData(request, targetCategories);
            
            return ResponseEntity.ok(
                    ApiResponse.success(newsData, "Category news data retrieved successfully."));
                    
        } catch (Exception e) {
            log.error("Failed to fetch category news content", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("CONTENT_FETCH_ERROR", "Failed to fetch category news content: " + e.getMessage()));
        }
    }

    /**
     * 구독자 정보 조회
     */
    @GetMapping("/content/subscribers")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getSubscriberInfo(
            @RequestParam List<Long> userIds) {
        
        log.info("Fetching subscriber information for userIds: {}", userIds);
        
        try {
            List<UserResponse> subscriberInfo = contentGenerationService.getSubscriberInfo(userIds);
            
            return ResponseEntity.ok(
                    ApiResponse.success(subscriberInfo, "Subscriber information retrieved successfully."));
                    
        } catch (Exception e) {
            log.error("Failed to fetch subscriber information", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SUBSCRIBER_FETCH_ERROR", "Failed to fetch subscriber information: " + e.getMessage()));
        }
    }

    /**
     * 사용 가능한 카테고리 조회
     */
    @GetMapping("/content/categories/available")
    public ResponseEntity<ApiResponse<Object>> getAvailableCategories() {
        
        log.info("Fetching available categories");
        
        try {
            List<NewsCategory> defaultCategories = contentGenerationService.getDefaultCategories();
            List<NewsCategory> personalizedCategories = contentGenerationService.getPersonalizedCategories();
            
            var response = Map.of(
                "defaultCategories", defaultCategories,
                "personalizedCategories", personalizedCategories
            );
            
            return ResponseEntity.ok(
                    ApiResponse.success(response, "Available categories retrieved successfully."));
                    
        } catch (Exception e) {
            log.error("Failed to fetch available categories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("CATEGORY_FETCH_ERROR", "Failed to fetch available categories: " + e.getMessage()));
        }
    }

    // ===== Existing Newsletter Delivery Endpoints =====

    /**
     * 뉴스레터 예약 발송
     */
    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<List<NewsletterDelivery>>> scheduleDelivery(
            @Valid @RequestBody NewsletterCreateRequest request) {

        log.info("Newsletter scheduling request: newsletterId={}, targetUserIds={}",
                request.getNewsletterId(), request.getTargetUserIds());

        try {
            List<NewsletterDelivery> responses = request.getTargetUserIds().stream()
                    .map(userId -> {
                        NewsletterDelivery requestEntity = NewsletterDelivery.builder()
                                .newsletterId(request.getNewsletterId())
                                .userId(userId)
                                .deliveryMethod(request.getDeliveryMethod())
                                .scheduledAt(parseScheduledAt(request.getScheduledAt()))
                                .build();
                        
                        return deliveryService.scheduleDelivery(requestEntity);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(responses, "Newsletter scheduling completed."));

        } catch (Exception e) {
            log.error("Newsletter scheduling failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("SCHEDULE_FAILED", "Newsletter scheduling failed: " + e.getMessage()));
        }
    }

    /**
     * 뉴스레터 즉시 발송
     */
    @PostMapping("/send-now")
    public ResponseEntity<ApiResponse<List<NewsletterDelivery>>> sendImmediately(
            @Valid @RequestBody NewsletterCreateRequest request) {

        log.info("Immediate newsletter delivery request: newsletterId={}, targetUserIds={}",
                request.getNewsletterId(), request.getTargetUserIds());

        try {
            List<NewsletterDelivery> responses = request.getTargetUserIds().stream()
                    .map(userId -> {
                        NewsletterDelivery requestEntity = NewsletterDelivery.builder()
                                .newsletterId(request.getNewsletterId())
                                .userId(userId)
                                .deliveryMethod(request.getDeliveryMethod())
                                .sentAt(LocalDateTime.now())
                                .build();
                        
                        return deliveryService.sendImmediately(requestEntity);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(
                    ApiResponse.success(responses, "Newsletter sent immediately."));

        } catch (Exception e) {
            log.error("Immediate newsletter delivery failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("SEND_FAILED", "Newsletter delivery failed: " + e.getMessage()));
        }
    }

    /**
     * 발송 상태별 조회
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<NewsletterDelivery>>> getDeliveriesByStatus(
            @PathVariable DeliveryStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Fetching deliveries by status: status={}, page={}", status, pageable.getPageNumber());

        try {
            Page<NewsletterDelivery> deliveries =
                    deliveryService.getDeliveriesByStatus(status, pageable);

            return ResponseEntity.ok(
                    ApiResponse.success(deliveries, "Delivery list retrieval completed."));

        } catch (Exception e) {
            log.error("Failed to fetch deliveries by status: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("QUERY_FAILED", "Failed to retrieve delivery list: " + e.getMessage()));
        }
    }

    /**
     * 사용자별 발송 이력 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NewsletterDelivery>>> getDeliveriesByUser(
             @PathVariable Long userId,
             @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Fetching delivery history by user: userId={}", userId);

        try {
            Page<NewsletterDelivery> deliveriesPage =
                    deliveryService.getDeliveriesByRecipient(userId, pageable);
            List<NewsletterDelivery> deliveries = deliveriesPage.getContent();

            return ResponseEntity.ok(
                    ApiResponse.success(deliveries, "Delivery history retrieval completed."));

        } catch (Exception e) {
            log.error("Failed to fetch delivery history by user: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("USER_QUERY_FAILED", "Failed to retrieve delivery history: " + e.getMessage()));
        }
    }

    /**
     * 뉴스레터별 발송 이력 조회
     */
    @GetMapping("/newsletter/{newsletterId}")
    public ResponseEntity<ApiResponse<List<NewsletterDelivery>>> getDeliveriesByNewsletter(
             @PathVariable Long newsletterId) {

        log.info("Fetching delivery history by newsletter: newsletterId={}", newsletterId);

        try {
            List<NewsletterDelivery> deliveries = deliveryService.getDeliveriesByNewsletter(newsletterId);

            return ResponseEntity.ok(
                    ApiResponse.success(deliveries, "Newsletter delivery history retrieval completed."));

        } catch (Exception e) {
            log.error("Failed to fetch newsletter delivery history: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("NEWSLETTER_QUERY_FAILED", "Failed to retrieve newsletter delivery history: " + e.getMessage()));
        }
    }

    /**
     * 예약된 발송 목록 조회
     */
    @GetMapping("/scheduled")
    public ResponseEntity<ApiResponse<List<NewsletterDelivery>>> getScheduledDeliveries() {

        log.info("Fetching scheduled deliveries request");

        try {
            List<NewsletterDelivery> scheduled = deliveryService.getScheduledDeliveries();

            return ResponseEntity.ok(
                    ApiResponse.success(scheduled, "Scheduled delivery list retrieval completed."));

        } catch (Exception e) {
            log.error("Failed to fetch scheduled deliveries: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SCHEDULED_QUERY_FAILED", "Failed to retrieve scheduled delivery list: " + e.getMessage()));
        }
    }

    /**
     * 발송 취소
     */
    @PutMapping("/{deliveryId}/cancel")
    public ResponseEntity<ApiResponse<NewsletterDelivery>> cancelDelivery(
            @PathVariable Long deliveryId) {

        log.info("Delivery cancellation request: deliveryId={}", deliveryId);

        try {
            NewsletterDelivery response = deliveryService.cancelDelivery(deliveryId);

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Newsletter delivery cancelled."));

        } catch (IllegalStateException e) {
            log.warn("Delivery cancellation not allowed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("CANCEL_NOT_ALLOWED", e.getMessage()));

        } catch (Exception e) {
            log.error("Delivery cancellation failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("CANCEL_FAILED", "Delivery cancellation failed: " + e.getMessage()));
        }
    }

    /**
     * 발송 재시도
     */
    @PutMapping("/{deliveryId}/retry")
    public ResponseEntity<ApiResponse<NewsletterDelivery>> retryDelivery(
             @PathVariable Long deliveryId) {

        log.info("Delivery retry request: deliveryId={}", deliveryId);

        try {
            NewsletterDelivery response = deliveryService.retryDelivery(deliveryId);

            return ResponseEntity.ok(
                    ApiResponse.success(response, "Newsletter delivery retry attempted."));

        } catch (IllegalStateException e) {
            log.warn("Delivery retry not allowed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("RETRY_NOT_ALLOWED", e.getMessage()));

        } catch (Exception e) {
            log.error("Delivery retry failed: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("RETRY_FAILED", "Delivery retry failed: " + e.getMessage()));
        }
    }

    /**
     * 발송 상세 조회
     */
    @GetMapping("/{deliveryId}")
    public ResponseEntity<ApiResponse<NewsletterDelivery>> getDeliveryDetail(
             @PathVariable Long deliveryId) {

        log.info("Fetching delivery detail: deliveryId={}", deliveryId);

        try {
            NewsletterDelivery delivery = deliveryService.getDeliveryDetail(deliveryId);

            return ResponseEntity.ok(
                    ApiResponse.success(delivery, "Delivery detail retrieval completed."));

        } catch (Exception e) {
            log.error("Failed to fetch delivery detail: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("DELIVERY_NOT_FOUND", "Delivery not found: " + e.getMessage()));
        }
    }

    /**
     * 발송 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DeliveryStats>> getDeliveryStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Fetching delivery statistics: {} ~ {}", startDate, endDate);

        try {
            DeliveryStats stats = deliveryService.getDeliveryStats(startDate, endDate);

            return ResponseEntity.ok(
                    ApiResponse.success(stats, "Delivery statistics retrieval completed."));

        } catch (Exception e) {
            log.error("Failed to fetch delivery statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("STATS_QUERY_FAILED", "Failed to retrieve delivery statistics: " + e.getMessage()));
        }
    }

    /**
     * 특정 기간 발송 기록 조회
     */
    @GetMapping("/period")
    public ResponseEntity<ApiResponse<List<NewsletterDelivery>>> getDeliveriesByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("Fetching deliveries by period: {} ~ {}", startDate, endDate);

        try {
            List<NewsletterDelivery> deliveries = deliveryService.getDeliveriesByPeriod(startDate, endDate);

            return ResponseEntity.ok(
                    ApiResponse.success(deliveries, "Period-based delivery record retrieval completed."));

        } catch (Exception e) {
            log.error("Failed to fetch deliveries by period: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("PERIOD_QUERY_FAILED", "Failed to retrieve period-based delivery records: " + e.getMessage()));
        }
    }

    /**
     * 사용자 열람률 조회
     */
    @GetMapping("/user/{userId}/open-rate")
    public ResponseEntity<ApiResponse<UserResponse>> getUserOpenRate(
             @PathVariable Long userId) {

        log.info("Fetching user open rate: userId={}", userId);

        try {
            Page<NewsletterDelivery> deliveriesPage = deliveryService.getDeliveriesByRecipient(userId, Pageable.unpaged());
            List<NewsletterDelivery> deliveries = deliveriesPage.getContent();
            
            Long totalSent = deliveries.stream()
                    .filter(d -> d.getStatus() == DeliveryStatus.SENT)
                    .count();

            Long totalOpened = deliveryService.countOpenedByUserId(userId);

            double openRate = totalSent > 0 ? (double) totalOpened / totalSent * 100 : 0.0;

            UserResponse response = new UserResponse();
            return ResponseEntity.ok(
                    ApiResponse.success(response, "User open rate retrieval completed."));

        } catch (Exception e) {
            log.error("Failed to fetch user open rate: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("OPEN_RATE_QUERY_FAILED", "Failed to retrieve user open rate: " + e.getMessage()));
        }
    }

    /**
     * 뉴스레터 성과 통계 조회
     */
    @GetMapping("/performance-stats")
    public ResponseEntity<ApiResponse<List<Object[]>>> getPerformanceStats() {

        log.info("Fetching newsletter performance statistics request");

        try {
            List<Object[]> stats = deliveryService.getNewsletterPerformanceStats();

            return ResponseEntity.ok(
                    ApiResponse.success(stats, "Newsletter performance statistics retrieval completed."));

        } catch (Exception e) {
            log.error("Failed to fetch newsletter performance statistics: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("PERFORMANCE_STATS_FAILED", "Failed to retrieve newsletter performance statistics: " + e.getMessage()));
        }
    }

    /**
     * 최근 실패 건수 조회
     */
    @GetMapping("/failed-count")
    public ResponseEntity<ApiResponse<Long>> getFailedCount(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate) {

        log.info("Fetching recent failure count: fromDate={}", fromDate);

        try {
            Long failedCount = deliveryService.countFailedDeliveriesAfter(fromDate);

            return ResponseEntity.ok(
                    ApiResponse.success(failedCount, "Failure count retrieval completed."));

        } catch (Exception e) {
            log.error("Failed to fetch failure count: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("FAILED_COUNT_QUERY_FAILED", "Failed to retrieve failure count: " + e.getMessage()));
        }
    }

    /**
     * 예약 시간 파싱 헬퍼 메서드
     */
    private LocalDateTime parseScheduledAt(String scheduledAt) {
        if (scheduledAt == null || scheduledAt.isEmpty()) {
            return LocalDateTime.now().plusHours(1); // 기본값: 1시간 후
        }
        
        try {
            return LocalDateTime.parse(scheduledAt, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (Exception e) {
            log.warn("Failed to parse scheduled time: {}, using default value", scheduledAt);
            return LocalDateTime.now().plusHours(1);
        }
    }
}