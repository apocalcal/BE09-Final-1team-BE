package com.newsletterservice.controller;

import com.newsletterservice.dto.NewsletterContent;
import com.newsletterservice.service.EmailNewsletterRenderer;
import com.newsletterservice.service.NewsletterContentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {
    
    private final NewsletterContentService contentService;
    private final EmailNewsletterRenderer emailRenderer;
    
    /**
     * 새로운 구조 테스트 - 개인화된 뉴스레터 콘텐츠 생성
     */
    @GetMapping("/newsletter/{newsletterId}/content")
    public ResponseEntity<NewsletterContent> testPersonalizedContent(@PathVariable Long newsletterId) {
        try {
            log.info("Testing personalized content generation for newsletter: {}", newsletterId);
            
            // 테스트용 사용자 ID (실제로는 인증에서 가져와야 함)
            Long testUserId = 1L;
            
            NewsletterContent content = contentService.buildPersonalizedContent(testUserId, newsletterId);
            
            log.info("Generated content: {} sections, {} total articles", 
                    content.getSections().size(),
                    content.getSections().stream()
                        .mapToInt(section -> section.getArticles().size())
                        .sum());
            
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            log.error("Error testing personalized content", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * 새로운 구조 테스트 - 이메일 HTML 생성
     */
    @GetMapping("/newsletter/{newsletterId}/html")
    public ResponseEntity<String> testEmailHtml(@PathVariable Long newsletterId) {
        try {
            log.info("Testing email HTML generation for newsletter: {}", newsletterId);
            
            // 테스트용 사용자 ID
            Long testUserId = 1L;
            
            NewsletterContent content = contentService.buildPersonalizedContent(testUserId, newsletterId);
            String html = emailRenderer.renderToHtml(content);
            
            log.info("Generated HTML content length: {} characters", html.length());
            
            return ResponseEntity.ok()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body(html);
        } catch (Exception e) {
            log.error("Error testing email HTML generation", e);
            return ResponseEntity.badRequest()
                .header("Content-Type", "text/html; charset=UTF-8")
                .body("<h1>오류가 발생했습니다</h1><p>" + e.getMessage() + "</p>");
        }
    }
    
    /**
     * 새로운 구조 테스트 - 카테고리별 콘텐츠
     */
    @GetMapping("/newsletter/{newsletterId}/category-test")
    public ResponseEntity<NewsletterContent> testCategoryContent(@PathVariable Long newsletterId) {
        try {
            log.info("Testing category content generation for newsletter: {}", newsletterId);
            
            // 테스트용 사용자 ID
            Long testUserId = 1L;
            
            // 테스트용 카테고리들
            var categories = List.of(
                com.newsletterservice.entity.NewsCategory.POLITICS,
                com.newsletterservice.entity.NewsCategory.ECONOMY
            );
            
            NewsletterContent content = contentService.buildCategoryContent(testUserId, newsletterId, categories);
            
            return ResponseEntity.ok(content);
        } catch (Exception e) {
            log.error("Error testing category content", e);
            return ResponseEntity.badRequest().build();
        }
    }
}
