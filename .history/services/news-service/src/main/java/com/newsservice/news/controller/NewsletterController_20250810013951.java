package com.newsservice.news.controller;

import com.newsservice.news.dto.NewsletterSubscribeRequest;
import com.newsservice.news.service.NewsletterService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/newsletter")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class NewsletterController {

    private final NewsletterService newsletterService;

    /**
     * 뉴스레터 구독
     */
    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@Valid @RequestBody NewsletterSubscribeRequest request,
                                     HttpServletRequest httpRequest) {
        try {
            // IP 주소 추출
            String ip = getClientIpAddress(httpRequest);
            request.setIp(ip);
            
            newsletterService.subscribe(request);
            return ResponseEntity.ok().body(Map.of("message", "구독 확인 메일을 발송했습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("뉴스레터 구독 처리 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "구독 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 구독 확인
     */
    @GetMapping("/confirm")
    public ResponseEntity<?> confirmSubscription(@RequestParam String token) {
        try {
            newsletterService.confirmSubscription(token);
            return ResponseEntity.ok().body(Map.of("message", "구독이 완료되었습니다."));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        } catch (Exception e) {
            log.error("구독 확인 처리 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "구독 확인 처리 중 오류가 발생했습니다."));
        }
    }

    /**
     * 확인된 구독자 수 조회
     */
    @GetMapping("/count")
    public ResponseEntity<?> getSubscriberCount() {
        try {
            long count = newsletterService.getConfirmedSubscriberCount();
            return ResponseEntity.ok().body(Map.of("count", count));
        } catch (Exception e) {
            log.error("구독자 수 조회 중 오류 발생", e);
            return ResponseEntity.internalServerError().body(Map.of("message", "구독자 수 조회 중 오류가 발생했습니다."));
        }
    }

    /**
     * 클라이언트 IP 주소 추출
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
