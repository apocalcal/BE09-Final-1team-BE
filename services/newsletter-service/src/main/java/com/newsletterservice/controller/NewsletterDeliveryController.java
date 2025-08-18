package com.newsletterservice.controller;

import com.newsletterservice.client.dto.UserResponse;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.dto.NewsletterCreateRequest;
import com.newsletterservice.dto.NewsletterDeliveryResponse;
import com.newsletterservice.dto.DeliveryStats;
import com.newsletterservice.entity.NewsletterDelivery;
import com.newsletterservice.entity.DeliveryStatus;
import com.newsletterservice.entity.DeliveryMethod;
import com.newsletterservice.service.NewsletterDeliveryService;

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
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/newsletter-delivery")
@RequiredArgsConstructor
public class NewsletterDeliveryController {

    private final NewsletterDeliveryService deliveryService;

    /**
     * 뉴스레터 예약 발송
     */
    @PostMapping("/schedule")
    public ResponseEntity<ApiResponse<List<NewsletterDelivery>>> scheduleDelivery(
            @Valid @RequestBody NewsletterCreateRequest request) {

        log.info("뉴스레터 예약 발송 요청: newsletterId={}, targetUserIds={}",
                request.getNewsletterId(), request.getTargetUserIds());

        try {
            List<NewsletterDelivery> responses = request.getTargetUserIds().stream()
                    .map(userId -> {
                        NewsletterDelivery requestEntity = NewsletterDelivery.builder()
                                .newsletterId(request.getNewsletterId())
                                .userId(userId)
                                .deliveryMethod(request.getDeliveryMethod())
                                .personalizedContent(request.isPersonalized() ? "개인화된 내용" : "일반 내용")
                                .scheduledAt(parseScheduledAt(request.getScheduledAt()))
                                .build();
                        
                        return deliveryService.scheduleDelivery(requestEntity);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success(responses, "뉴스레터 예약이 완료되었습니다."));

        } catch (Exception e) {
            log.error("뉴스레터 예약 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("SCHEDULE_FAILED", "뉴스레터 예약에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 뉴스레터 즉시 발송
     */
    @PostMapping("/send-now")
    public ResponseEntity<ApiResponse<List<NewsletterDelivery>>> sendImmediately(
            @Valid @RequestBody NewsletterCreateRequest request) {

        log.info("뉴스레터 즉시 발송 요청: newsletterId={}, targetUserIds={}",
                request.getNewsletterId(), request.getTargetUserIds());

        try {
            List<NewsletterDelivery> responses = request.getTargetUserIds().stream()
                    .map(userId -> {
                        NewsletterDelivery requestEntity = NewsletterDelivery.builder()
                                .newsletterId(request.getNewsletterId())
                                .userId(userId)
                                .personalizedContent(request.isPersonalized() ? "개인화된 내용" : "일반 내용")
                                .deliveryMethod(request.getDeliveryMethod())
                                .sentAt(LocalDateTime.now())
                                .build();
                        
                        return deliveryService.sendImmediately(requestEntity);
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(
                    ApiResponse.success(responses, "뉴스레터가 즉시 발송되었습니다."));

        } catch (Exception e) {
            log.error("뉴스레터 즉시 발송 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("SEND_FAILED", "뉴스레터 발송에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 발송 상태별 조회
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<Page<NewsletterDelivery>>> getDeliveriesByStatus(
            @PathVariable DeliveryStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("발송 상태별 조회 요청: status={}, page={}", status, pageable.getPageNumber());

        try {
            Page<NewsletterDelivery> deliveries =
                    deliveryService.getDeliveriesByStatus(status, pageable);

            return ResponseEntity.ok(
                    ApiResponse.success(deliveries, "발송 목록 조회가 완료되었습니다."));

        } catch (Exception e) {
            log.error("발송 상태별 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("QUERY_FAILED", "발송 목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 사용자별 발송 이력 조회
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<NewsletterDelivery>>> getDeliveriesByUser(
             @PathVariable Long userId) {

        log.info("사용자별 발송 이력 조회: userId={}", userId);

        try {
            List<NewsletterDelivery> deliveries =
                    deliveryService.getDeliveryRepository().findByUserIdOrderByCreatedAtDesc(userId);

            return ResponseEntity.ok(
                    ApiResponse.success(deliveries, "발송 이력 조회가 완료되었습니다."));

        } catch (Exception e) {
            log.error("사용자별 발송 이력 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("USER_QUERY_FAILED", "발송 이력 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 뉴스레터별 발송 이력 조회
     */
    @GetMapping("/newsletter/{newsletterId}")
    public ResponseEntity<ApiResponse<List<NewsletterDelivery>>> getDeliveriesByNewsletter(
             @PathVariable Long newsletterId) {

        log.info("뉴스레터별 발송 이력 조회: newsletterId={}", newsletterId);

        try {
            List<NewsletterDelivery> deliveries =
                    deliveryService.getDeliveryRepository().findByNewsletterIdOrderByCreatedAtDesc(newsletterId);

            return ResponseEntity.ok(
                    ApiResponse.success(deliveries, "뉴스레터별 발송 이력 조회가 완료되었습니다."));

        } catch (Exception e) {
            log.error("뉴스레터별 발송 이력 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("NEWSLETTER_QUERY_FAILED", "뉴스레터별 발송 이력 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 예약된 발송 목록 조회
     */
    @GetMapping("/scheduled")
    public ResponseEntity<ApiResponse<List<NewsletterDelivery>>> getScheduledDeliveries() {

        log.info("예약된 발송 목록 조회 요청");

        try {
            List<NewsletterDelivery> scheduled = deliveryService.getScheduledDeliveries();

            return ResponseEntity.ok(
                    ApiResponse.success(scheduled, "예약된 발송 목록 조회가 완료되었습니다."));

        } catch (Exception e) {
            log.error("예약된 발송 목록 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("SCHEDULED_QUERY_FAILED", "예약된 발송 목록 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 발송 취소
     */
    @PutMapping("/{deliveryId}/cancel")
    public ResponseEntity<ApiResponse<NewsletterDelivery>> cancelDelivery(
            @PathVariable Long deliveryId) {

        log.info("발송 취소 요청: deliveryId={}", deliveryId);

        try {
            NewsletterDelivery response = deliveryService.cancelDelivery(deliveryId);

            return ResponseEntity.ok(
                    ApiResponse.success(response, "뉴스레터 발송이 취소되었습니다."));

        } catch (IllegalStateException e) {
            log.warn("발송 취소 불가: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("CANCEL_NOT_ALLOWED", e.getMessage()));

        } catch (Exception e) {
            log.error("발송 취소 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("CANCEL_FAILED", "발송 취소에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 발송 재시도
     */
    @PutMapping("/{deliveryId}/retry")
    public ResponseEntity<ApiResponse<NewsletterDelivery>> retryDelivery(
             @PathVariable Long deliveryId) {

        log.info("발송 재시도 요청: deliveryId={}", deliveryId);

        try {
            NewsletterDelivery response = deliveryService.retryDelivery(deliveryId);

            return ResponseEntity.ok(
                    ApiResponse.success(response, "뉴스레터 발송을 재시도했습니다."));

        } catch (IllegalStateException e) {
            log.warn("발송 재시도 불가: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("RETRY_NOT_ALLOWED", e.getMessage()));

        } catch (Exception e) {
            log.error("발송 재시도 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("RETRY_FAILED", "발송 재시도에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 발송 상세 조회
     */
    @GetMapping("/{deliveryId}")
    public ResponseEntity<ApiResponse<NewsletterDelivery>> getDeliveryDetail(
             @PathVariable Long deliveryId) {

        log.info("발송 상세 조회: deliveryId={}", deliveryId);

        try {
            NewsletterDelivery delivery = deliveryService.getDeliveryRepository()
                    .findById(deliveryId)
                    .orElseThrow(() -> new RuntimeException("발송 정보를 찾을 수 없습니다."));

            return ResponseEntity.ok(
                    ApiResponse.success(delivery, "발송 상세 정보 조회가 완료되었습니다."));

        } catch (Exception e) {
            log.error("발송 상세 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("DELIVERY_NOT_FOUND", "발송 정보를 찾을 수 없습니다: " + e.getMessage()));
        }
    }

    /**
     * 발송 통계 조회
     */
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DeliveryStats>> getDeliveryStats(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("발송 통계 조회: {} ~ {}", startDate, endDate);

        try {
            DeliveryStats stats = deliveryService.getDeliveryStats(startDate, endDate);

            return ResponseEntity.ok(
                    ApiResponse.success(stats, "발송 통계 조회가 완료되었습니다."));

        } catch (Exception e) {
            log.error("발송 통계 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("STATS_QUERY_FAILED", "발송 통계 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 특정 기간 발송 기록 조회
     */
    @GetMapping("/period")
    public ResponseEntity<ApiResponse<List<NewsletterDelivery>>> getDeliveriesByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        log.info("기간별 발송 기록 조회: {} ~ {}", startDate, endDate);

        try {
            List<NewsletterDelivery> deliveries = deliveryService.getDeliveryRepository()
                    .findBySentAtBetween(startDate, endDate);

            return ResponseEntity.ok(
                    ApiResponse.success(deliveries, "기간별 발송 기록 조회가 완료되었습니다."));

        } catch (Exception e) {
            log.error("기간별 발송 기록 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("PERIOD_QUERY_FAILED", "기간별 발송 기록 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 사용자 열람률 조회
     */
    @GetMapping("/user/{userId}/open-rate")
    public ResponseEntity<ApiResponse<UserResponse>> getUserOpenRate(
             @PathVariable Long userId) {

        log.info("사용자 열람률 조회: userId={}", userId);

        try {
            Long totalSent = deliveryService.getDeliveryRepository()
                    .findByUserIdOrderByCreatedAtDesc(userId).stream()
                    .filter(d -> d.getStatus() == DeliveryStatus.SENT)
                    .count();

            Long totalOpened = deliveryService.getDeliveryRepository()
                    .countOpenedByUserId(userId);

            double openRate = totalSent > 0 ? (double) totalOpened / totalSent * 100 : 0.0;

            UserResponse response = new UserResponse();
            return ResponseEntity.ok(
                    ApiResponse.success(response, "사용자 열람률 조회가 완료되었습니다."));

        } catch (Exception e) {
            log.error("사용자 열람률 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("OPEN_RATE_QUERY_FAILED", "사용자 열람률 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 뉴스레터 성과 통계 조회
     */
    @GetMapping("/performance-stats")
    public ResponseEntity<ApiResponse<List<Object[]>>> getPerformanceStats() {

        log.info("뉴스레터 성과 통계 조회 요청");

        try {
            List<Object[]> stats = deliveryService.getDeliveryRepository().getNewsletterPerformanceStats();

            return ResponseEntity.ok(
                    ApiResponse.success(stats, "뉴스레터 성과 통계 조회가 완료되었습니다."));

        } catch (Exception e) {
            log.error("뉴스레터 성과 통계 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("PERFORMANCE_STATS_FAILED", "뉴스레터 성과 통계 조회에 실패했습니다: " + e.getMessage()));
        }
    }

    /**
     * 최근 실패 건수 조회
     */
    @GetMapping("/failed-count")
    public ResponseEntity<ApiResponse<Long>> getFailedCount(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate) {

        log.info("최근 실패 건수 조회: fromDate={}", fromDate);

        try {
            Long failedCount = deliveryService.getDeliveryRepository().countFailedDeliveriesAfter(fromDate);

            return ResponseEntity.ok(
                    ApiResponse.success(failedCount, "실패 건수 조회가 완료되었습니다."));

        } catch (Exception e) {
            log.error("실패 건수 조회 실패: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("FAILED_COUNT_QUERY_FAILED", "실패 건수 조회에 실패했습니다: " + e.getMessage()));
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
            log.warn("예약 시간 파싱 실패: {}, 기본값 사용", scheduledAt);
            return LocalDateTime.now().plusHours(1);
        }
    }
}