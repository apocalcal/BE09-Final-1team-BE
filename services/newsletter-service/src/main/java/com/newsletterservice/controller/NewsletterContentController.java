package com.newsletterservice.controller;

import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.dto.NewsletterContent;
import com.newsletterservice.entity.NewsCategory;
import com.newsletterservice.service.EmailNewsletterRenderer;
import com.newsletterservice.service.NewsletterContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
@Slf4j
public class NewsletterContentController {
    
    private final NewsletterContentService contentService;
    private final EmailNewsletterRenderer emailRenderer;
    
    /**
     * 개인화된 뉴스레터 콘텐츠 조회 (JSON)
     */
    @GetMapping("/{newsletterId}/content")
    public ResponseEntity<ApiResponse<NewsletterContent>> getNewsletterContent(
            @PathVariable Long newsletterId,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuth(authentication);
            log.info("Getting newsletter content for user: {}, newsletter: {}", userId, newsletterId);
            
            NewsletterContent content = contentService.buildPersonalizedContent(userId, newsletterId);
            
            return ResponseEntity.ok(ApiResponse.success(content));
        } catch (Exception e) {
            log.error("Error getting newsletter content", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("CONTENT_FETCH_ERROR", "뉴스레터 콘텐츠 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 개인화된 뉴스레터 HTML 조회 (이메일용)
     */
    @GetMapping("/{newsletterId}/html")
    public ResponseEntity<ApiResponse<String>> getNewsletterHtml(
            @PathVariable Long newsletterId,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuth(authentication);
            log.info("Getting newsletter HTML for user: {}, newsletter: {}", userId, newsletterId);
            
            NewsletterContent content = contentService.buildPersonalizedContent(userId, newsletterId);
            String html = emailRenderer.renderToHtml(content);
            
            return ResponseEntity.ok(ApiResponse.success(html));
        } catch (Exception e) {
            log.error("Error getting newsletter HTML", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("HTML_GENERATION_ERROR", "뉴스레터 HTML 생성 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 카테고리별 뉴스레터 콘텐츠 조회
     */
    @PostMapping("/{newsletterId}/category-content")
    public ResponseEntity<ApiResponse<NewsletterContent>> getCategoryNewsletterContent(
            @PathVariable Long newsletterId,
            @RequestBody List<NewsCategory> categories,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuth(authentication);
            log.info("Getting category newsletter content for user: {}, newsletter: {}, categories: {}", 
                    userId, newsletterId, categories);
            
            NewsletterContent content = contentService.buildCategoryContent(userId, newsletterId, categories);
            
            return ResponseEntity.ok(ApiResponse.success(content));
        } catch (Exception e) {
            log.error("Error getting category newsletter content", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("CATEGORY_CONTENT_ERROR", "카테고리 뉴스레터 콘텐츠 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 최신 뉴스 기반 뉴스레터 콘텐츠 조회
     */
    @GetMapping("/{newsletterId}/latest-content")
    public ResponseEntity<ApiResponse<NewsletterContent>> getLatestNewsletterContent(
            @PathVariable Long newsletterId,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuth(authentication);
            log.info("Getting latest newsletter content for user: {}, newsletter: {}", userId, newsletterId);
            
            NewsletterContent content = contentService.buildLatestContent(userId, newsletterId);
            
            return ResponseEntity.ok(ApiResponse.success(content));
        } catch (Exception e) {
            log.error("Error getting latest newsletter content", e);
            return ResponseEntity.badRequest()
                .body(ApiResponse.error("LATEST_CONTENT_ERROR", "최신 뉴스레터 콘텐츠 조회 중 오류가 발생했습니다."));
        }
    }
    
    /**
     * 뉴스레터 미리보기 (HTML)
     */
    @GetMapping("/{newsletterId}/preview")
    public ResponseEntity<String> getNewsletterPreview(
            @PathVariable Long newsletterId,
            Authentication authentication) {
        
        try {
            Long userId = getUserIdFromAuth(authentication);
            log.info("Getting newsletter preview for user: {}, newsletter: {}", userId, newsletterId);
            
            NewsletterContent content = contentService.buildPersonalizedContent(userId, newsletterId);
            String html = emailRenderer.renderToHtml(content);
            
            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
        } catch (Exception e) {
            log.error("Error getting newsletter preview", e);
            return ResponseEntity.badRequest()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body("<h1>오류가 발생했습니다</h1><p>" + e.getMessage() + "</p>");
        }
    }
    
    // ===== Private helper methods =====
    
    private Long getUserIdFromAuth(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new IllegalArgumentException("인증 정보가 없습니다.");
        }
        
        // 실제 구현에서는 JWT 토큰에서 userId를 추출해야 함
        // 여기서는 임시로 1L을 반환
        return 1L;
    }
}
