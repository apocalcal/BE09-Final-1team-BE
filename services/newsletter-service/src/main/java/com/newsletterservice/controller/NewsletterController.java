package com.newsletterservice.controller;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.common.exception.NewsletterException;
import com.newsletterservice.dto.*;
import com.newsletterservice.repository.NewsletterDeliveryRepository;
import com.newsletterservice.service.EmailNewsletterRenderer;
import com.newsletterservice.service.NewsletterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NewsletterController {

    // ========================================
    // Service Dependencies
    // ========================================
    private final NewsletterService newsletterService;
    private final EmailNewsletterRenderer emailRenderer;
    private final NewsletterDeliveryRepository deliveryRepository;

    // ========================================
    // 1. 구독 관리 기능
    // ========================================

    /**
     * 뉴스레터 구독
     */
    @PostMapping("/subscribe")
    public ApiResponse<SubscriptionResponse> subscribe(
            @Valid @RequestBody SubscriptionRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            request.setUserId(Long.valueOf(userId));
            log.info("구독 요청: userId={}, email={}, frequency={}, categories={}", 
                    request.getUserId(), request.getEmail(), request.getFrequency(), request.getPreferredCategories());
            SubscriptionResponse subscription = newsletterService.subscribe(request, userId);
            return ApiResponse.success(subscription, "구독이 완료되었습니다.");
        } catch (Exception e) {
            log.error("구독 처리 중 오류 발생", e);
            return ApiResponse.error("SUBSCRIPTION_ERROR", "구독 처리 중 오류가 발생했습니다.");
        }
    }

    /**
     * 구독 정보 조회
     */
    @GetMapping("/subscription/{id}")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> getSubscription(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("구독 정보 조회 요청 - userId: {}, subscriptionId: {}", userId, id);
            
            SubscriptionResponse subscription = newsletterService.getSubscription(id, Long.valueOf(userId));
            
            return ResponseEntity.ok(ApiResponse.success(subscription));
        } catch (NewsletterException e) {
            log.warn("구독 정보 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("구독 정보 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SUBSCRIPTION_FETCH_ERROR", "구독 정보 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 내 구독 목록 조회
     */
    @GetMapping("/subscription/my")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getMySubscriptions(
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("내 구독 목록 조회 요청 - userId: {}", userId);
            
            List<SubscriptionResponse> subscriptions = newsletterService.getMySubscriptions(Long.valueOf(userId));
            
            return ResponseEntity.ok(ApiResponse.success(subscriptions));
        } catch (Exception e) {
            log.error("내 구독 목록 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SUBSCRIPTION_LIST_ERROR", "구독 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 구독 해지
     */
    @DeleteMapping("/subscription/{id}")
    public ResponseEntity<ApiResponse<String>> unsubscribe(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("구독 해지 요청 - userId: {}, subscriptionId: {}", userId, id);
            
            newsletterService.unsubscribe(id, Long.valueOf(userId));
            
            return ResponseEntity.ok(ApiResponse.success("구독이 해지되었습니다."));
        } catch (NewsletterException e) {
            log.warn("구독 해지 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("구독 해지 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("UNSUBSCRIBE_ERROR", "구독 해지 중 오류가 발생했습니다."));
        }
    }

    /**
     * 활성 구독 목록 조회
     */
    @GetMapping("/subscription/active")
    public ResponseEntity<ApiResponse<List<SubscriptionResponse>>> getActiveSubscriptions(
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("활성 구독 목록 조회 요청 - userId: {}", userId);
            
            List<SubscriptionResponse> subscriptions = newsletterService.getActiveSubscriptions(Long.valueOf(userId));
            
            return ResponseEntity.ok(ApiResponse.success(subscriptions));
        } catch (Exception e) {
            log.error("활성 구독 목록 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("ACTIVE_SUBSCRIPTION_ERROR", "활성 구독 목록 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 구독 상태 변경
     */
    @PutMapping("/subscription/{subscriptionId}/status")
    public ResponseEntity<ApiResponse<SubscriptionResponse>> changeSubscriptionStatus(
            @PathVariable Long subscriptionId,
            @RequestBody Map<String, String> request,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            String newStatus = request.get("status");
            
            if (newStatus == null || newStatus.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("INVALID_STATUS", "상태값이 필요합니다."));
            }
            
            log.info("구독 상태 변경 요청 - userId: {}, subscriptionId: {}, newStatus: {}", 
                    userId, subscriptionId, newStatus);
            
            SubscriptionResponse subscription = newsletterService.changeSubscriptionStatus(
                    subscriptionId, Long.valueOf(userId), newStatus);
            
            return ResponseEntity.ok(ApiResponse.success(subscription, "구독 상태가 변경되었습니다."));
        } catch (NewsletterException e) {
            log.warn("구독 상태 변경 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("구독 상태 변경 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("STATUS_CHANGE_ERROR", "구독 상태 변경 중 오류가 발생했습니다."));
        }
    }

    /**
     * 개인화된 뉴스레터 콘텐츠 조회 (JSON)
     */
    @GetMapping("/{newsletterId}/content")
    public ResponseEntity<ApiResponse<NewsletterContent>> getNewsletterContent(
            @PathVariable Long newsletterId,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("퍼스널라이즈드 뉴스레터 콘텐츠 조회 - userId: {}, newsletterId: {}", userId, newsletterId);
            
            NewsletterContent content = newsletterService.buildPersonalizedContent(Long.valueOf(userId), newsletterId);
            
            return ResponseEntity.ok(ApiResponse.success(content));
        } catch (NewsletterException e) {
            log.warn("뉴스레터 콘텐츠 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("뉴스레터 콘텐츠 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("CONTENT_FETCH_ERROR", "뉴스레터 콘텐츠 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 개인화된 뉴스레터 HTML 조회 (이메일용)
     */
    @GetMapping("/{newsletterId}/html")
    public ResponseEntity<String> getNewsletterHtml(
            @PathVariable Long newsletterId,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("퍼스널라이즈드 뉴스레터 HTML 조회 - userId: {}, newsletterId: {}", userId, newsletterId);
            
            NewsletterContent content = newsletterService.buildPersonalizedContent(Long.valueOf(userId), newsletterId);
            String htmlContent = emailRenderer.renderToHtml(content);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(htmlContent);
        } catch (NewsletterException e) {
            log.warn("뉴스레터 HTML 조회 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body("<html><body><h1>오류</h1><p>" + e.getMessage() + "</p></body></html>");
        } catch (Exception e) {
            log.error("뉴스레터 HTML 조회 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body("<html><body><h1>오류</h1><p>뉴스레터 HTML 조회 중 오류가 발생했습니다.</p></body></html>");
        }
    }

    /**
     * 개인화된 뉴스레터 미리보기 (HTML)
     */
    @GetMapping("/{newsletterId}/preview")
    public ResponseEntity<String> getNewsletterPreview(
            @PathVariable Long newsletterId,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("퍼스널라이즈드 뉴스레터 미리보기 - userId: {}, newsletterId: {}", userId, newsletterId);
            
            NewsletterContent content = newsletterService.buildPersonalizedContent(Long.valueOf(userId), newsletterId);
            String previewHtml = emailRenderer.renderToPreviewHtml(content);
            
            return ResponseEntity.ok()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body(previewHtml);
        } catch (NewsletterException e) {
            log.warn("뉴스레터 미리보기 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body("<html><body><h1>오류</h1><p>" + e.getMessage() + "</p></body></html>");
        } catch (Exception e) {
            log.error("뉴스레터 미리보기 중 오류 발생", e);
            return ResponseEntity.badRequest()
                    .header("Content-Type", "text/html; charset=UTF-8")
                    .body("<html><body><h1>오류</h1><p>뉴스레터 미리보기 중 오류가 발생했습니다.</p></body></html>");
        }
    }

    // ========================================
    // 2. 발송 관리 기능
    // ========================================

    /**
     * 뉴스레터 즉시 발송
     */
    @PostMapping("/delivery/send-now")
    public ResponseEntity<ApiResponse<DeliveryStats>> sendNewsletterNow(
            @Valid @RequestBody NewsletterDeliveryRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("뉴스레터 즉시 발송 요청 - userId: {}, newsletterId: {}, targetUserIds: {}, deliveryMethod: {}", 
                    userId, request.getNewsletterId(), request.getTargetUserIds(), request.getDeliveryMethod());
            
            DeliveryStats stats = newsletterService.sendNewsletterNow(request, Long.valueOf(userId));
            
            return ResponseEntity.ok(ApiResponse.success(stats, "뉴스레터 발송이 시작되었습니다."));
        } catch (NewsletterException e) {
            log.warn("뉴스레터 발송 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("뉴스레터 발송 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("DELIVERY_ERROR", "뉴스레터 발송 중 오류가 발생했습니다."));
        }
    }

    /**
     * 뉴스레터 예약 발송
     */
    @PostMapping("/delivery/schedule")
    public ResponseEntity<ApiResponse<DeliveryStats>> scheduleNewsletter(
            @Valid @RequestBody NewsletterDeliveryRequest request,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("뉴스레터 예약 발송 요청 - userId: {}, newsletterId: {}, targetUserIds: {}, deliveryMethod: {}", 
                    userId, request.getNewsletterId(), request.getTargetUserIds(), request.getDeliveryMethod());
            
            DeliveryStats stats = newsletterService.scheduleNewsletter(request, Long.valueOf(userId));
            
            return ResponseEntity.ok(ApiResponse.success(stats, "뉴스레터가 예약되었습니다."));
        } catch (NewsletterException e) {
            log.warn("뉴스레터 예약 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("뉴스레터 예약 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("SCHEDULE_ERROR", "뉴스레터 예약 중 오류가 발생했습니다."));
        }
    }

    /**
     * 발송 취소
     */
    @PutMapping("/delivery/{deliveryId}/cancel")
    public ResponseEntity<ApiResponse<String>> cancelDelivery(
            @PathVariable Long deliveryId,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("발송 취소 요청 - userId: {}, deliveryId: {}", userId, deliveryId);
            
            newsletterService.cancelDelivery(deliveryId, Long.valueOf(userId));
            
            return ResponseEntity.ok(ApiResponse.success("발송이 취소되었습니다."));
        } catch (NewsletterException e) {
            log.warn("발송 취소 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("발송 취소 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("CANCEL_ERROR", "발송 취소 중 오류가 발생했습니다."));
        }
    }

    /**
     * 발송 재시도
     */
    @PutMapping("/delivery/{deliveryId}/retry")
    public ResponseEntity<ApiResponse<String>> retryDelivery(
        @PathVariable Long deliveryId,
            HttpServletRequest httpRequest) {
        
        try {
            String userId = extractUserIdFromToken(httpRequest);
            log.info("발송 재시도 요청 - userId: {}, deliveryId: {}", userId, deliveryId);
            
            newsletterService.retryDelivery(deliveryId, Long.valueOf(userId));
            
            return ResponseEntity.ok(ApiResponse.success("발송 재시도가 시작되었습니다."));
        } catch (NewsletterException e) {
            log.warn("발송 재시도 실패: {}", e.getMessage());
            return ResponseEntity.badRequest()
                .body(ApiResponse.error(e.getErrorCode(), e.getMessage()));
        } catch (Exception e) {
            log.error("발송 재시도 중 오류 발생", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("RETRY_ERROR", "발송 재시도 중 오류가 발생했습니다."));
        }
    }

    // ========================================
    // Private Helper Methods
    // ========================================

    /**
     * JWT 토큰에서 사용자 ID 추출
     */
    private String extractUserIdFromToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            log.info("Authorization 헤더에서 토큰 추출: {}", token);
            // TODO: 실제 JWT 토큰 파싱 로직 구
            // 현재는 임시로 토큰이 있으면 userId 1을 반환
            return "1";
        }
        // Authorization 헤더가 없으면 기본값 반환
        log.warn("Authorization 헤더가 없습니다. 기본 userId 사용");
        return "1";
    }

    /**
     * 인증 정보에서 사용자 ID 추출
     */
    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new NewsletterException("인증 정보가 없습니다.", "AUTHENTICATION_REQUIRED");
        }
        
        // 실제 구현에서는 JWT 토큰에서 userId를 추출해야 함
        // 여기서는 임시로 1L을 반환
        return 1L;
    }
}
