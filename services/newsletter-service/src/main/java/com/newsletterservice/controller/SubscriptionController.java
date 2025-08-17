package com.newsletterservice.controller;

import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.dto.SubscriptionRequest;
import com.newsletterservice.dto.SubscriptionResponse;
import com.newsletterservice.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class SubscriptionController {
    
    private final SubscriptionService subscriptionService;
    
    @PostMapping
    public ApiResponse<SubscriptionResponse> subscribe(
            @Valid @RequestBody SubscriptionRequest request,
            HttpServletRequest httpRequest) {
        
        String userId = extractUserIdFromToken(httpRequest);
        SubscriptionResponse subscription = subscriptionService.subscribe(request, userId);
        return ApiResponse.success(subscription, "구독이 완료되었습니다.");
    }
    
    @GetMapping("/{id}")
    public ApiResponse<SubscriptionResponse> getSubscription(@PathVariable Long id) {
        SubscriptionResponse subscription = subscriptionService.getSubscription(id);
        return ApiResponse.success(subscription);
    }
    
    @GetMapping("/user")
    public ApiResponse<List<SubscriptionResponse>> getMySubscriptions(HttpServletRequest httpRequest) {
        String userId = extractUserIdFromToken(httpRequest);
        List<SubscriptionResponse> subscriptions = subscriptionService.getSubscriptionsByUser(userId);
        return ApiResponse.success(subscriptions);
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> unsubscribe(@PathVariable Long id) {
        subscriptionService.unsubscribe(id);
        return ApiResponse.success(null, "구독이 해지되었습니다.");
    }
    
    @GetMapping("/active")
    public ApiResponse<List<SubscriptionResponse>> getActiveSubscriptions() {
        List<SubscriptionResponse> subscriptions = subscriptionService.getActiveSubscriptions();
        return ApiResponse.success(subscriptions);
    }
    
    // JWT 토큰에서 사용자 ID 추출 (실제 구현에서는 JWT 파싱 로직 필요)
    private String extractUserIdFromToken(HttpServletRequest request) {
        // TODO: JWT 토큰에서 사용자 ID 추출 로직 구현
        return "temp-user-id";
    }
}
