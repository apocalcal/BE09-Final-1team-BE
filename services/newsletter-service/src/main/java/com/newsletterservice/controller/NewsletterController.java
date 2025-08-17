package com.newsletterservice.controller;

import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.dto.NewsletterCreateRequest;
import com.newsletterservice.service.NewsletterDeliveryService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/newsletters")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class NewsletterController {
    
    private final NewsletterDeliveryService newsletterService;
    
    @GetMapping
    public ApiResponse<List<NewsletterResponse>> getNewsletters(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        List<NewsletterResponse> newsletters = newsletterService.getNewsletters(page, size);
        return ApiResponse.success(newsletters);
    }
    
    @GetMapping("/{id}")
    public ApiResponse<NewsletterResponse> getNewsletter(@PathVariable Long id) {
        NewsletterResponse newsletter = newsletterService.getNewsletter(id);
        return ApiResponse.success(newsletter);
    }
    
    @PostMapping
    public ApiResponse<NewsletterResponse> createNewsletter(
            @Valid @RequestBody NewsletterCreateRequest request,
            HttpServletRequest httpRequest) {
        
        String userId = extractUserIdFromToken(httpRequest);
        NewsletterResponse newsletter = newsletterService.createNewsletter(request, userId);
        return ApiResponse.success(newsletter);
    }
    
    @PostMapping("/personalized")
    public ApiResponse<NewsletterResponse> createPersonalizedNewsletter(
            HttpServletRequest httpRequest) {
        
        String userId = extractUserIdFromToken(httpRequest);
        NewsletterResponse newsletter = newsletterService.createPersonalizedNewsletter(Long.valueOf(userId));
        return ApiResponse.success(newsletter);
    }
    
    @PostMapping("/{id}/publish")
    public ApiResponse<NewsletterResponse> publishNewsletter(@PathVariable Long id) {
        NewsletterResponse newsletter = newsletterService.publishNewsletter(id);
        return ApiResponse.success(newsletter);
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteNewsletter(@PathVariable Long id) {
        newsletterService.deleteNewsletter(id);
        return ApiResponse.success(null, "뉴스레터가 삭제되었습니다.");
    }
    
    // JWT 토큰에서 사용자 ID 추출 (실제 구현에서는 JWT 파싱 로직 필요)
    private String extractUserIdFromToken(HttpServletRequest request) {
        // TODO: JWT 토큰에서 사용자 ID 추출 로직 구현
        return "temp-user-id";
    }
}
