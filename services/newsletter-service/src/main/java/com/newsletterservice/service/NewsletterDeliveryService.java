package com.newsletterservice.service;


import com.newsletterservice.dto.DeliveryStats;
import com.newsletterservice.dto.NewsletterContent;
import com.newsletterservice.entity.NewsletterDelivery;
import com.newsletterservice.entity.DeliveryStatus;
import com.newsletterservice.entity.DeliveryMethod;
import com.newsletterservice.entity.SubscriptionFrequency;
import com.newsletterservice.repository.NewsletterDeliveryRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
@Getter
public class NewsletterDeliveryService {

    private final NewsletterDeliveryRepository deliveryRepository;
    private final NewsletterContentService contentService;
    private final EmailNewsletterRenderer emailRenderer;

    /**
     * 뉴스레터 발송 예약
     */
    public NewsletterDelivery scheduleDelivery(NewsletterDelivery requestDTO) {
        log.info("Newsletter delivery scheduling started: {}", requestDTO.getId());

        NewsletterDelivery delivery = NewsletterDelivery.builder()
                .newsletterId(requestDTO.getNewsletterId())
                .userId(requestDTO.getUserId())
                .sentAt(requestDTO.getSentAt())
                .openedAt(requestDTO.getOpenedAt())
                .status(DeliveryStatus.PENDING)
                .deliveryMethod(requestDTO.getDeliveryMethod())
                .scheduledAt(requestDTO.getScheduledAt())
                .build();

        NewsletterDelivery saved = deliveryRepository.save(delivery);
        log.info("Newsletter delivery scheduling completed: ID={}", saved.getId());

        return saved;
    }

    /**
     * 즉시 발송
     */
    public NewsletterDelivery sendImmediately(NewsletterDelivery requestDTO) {
        log.info("Immediate newsletter delivery started: {}", requestDTO.getId());

        NewsletterDelivery delivery = NewsletterDelivery.builder()
                .newsletterId(requestDTO.getNewsletterId())
                .userId(requestDTO.getUserId())
                .sentAt(requestDTO.getSentAt())
                .openedAt(requestDTO.getOpenedAt())
                .status(DeliveryStatus.PENDING)
                .deliveryMethod(requestDTO.getDeliveryMethod())
                .build();

        NewsletterDelivery saved = deliveryRepository.save(delivery);

        // 실제 발송 로직 수행
        performDelivery(saved);

        return saved;
    }

    /**
     * 발송 상태별 조회
     */
    @Transactional(readOnly = true)
    public Page<NewsletterDelivery> getDeliveriesByStatus(
            DeliveryStatus status, Pageable pageable) {

        log.info("Fetching deliveries by status: status={}", status);

        return deliveryRepository.findByStatus(status, pageable);
    }

    /**
     * 수신자별 발송 이력 조회
     */
    @Transactional(readOnly = true)
    public Page<NewsletterDelivery> getDeliveriesByRecipient(
            Long userId, Pageable pageable) {

        log.info("Fetching delivery history by recipient: userId={}", userId);

        return deliveryRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * 예약된 발송 목록 조회
     */
    @Transactional(readOnly = true)
    public List<NewsletterDelivery> getScheduledDeliveries() {
        log.info("Fetching scheduled deliveries");

        return deliveryRepository.findByStatusAndScheduledAtBefore(
                DeliveryStatus.PENDING, LocalDateTime.now());
    }

    /**
     * 발송 취소
     */
    public NewsletterDelivery cancelDelivery(Long deliveryId) {
        log.info("Canceling delivery: ID={}", deliveryId);

        NewsletterDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new EntityNotFoundException("Delivery not found: " + deliveryId));

        if (delivery.getStatus() == DeliveryStatus.SENT) {
            throw new IllegalStateException("Cannot cancel already sent newsletter.");
        }

        delivery.updateStatus(DeliveryStatus.BOUNCED);
        NewsletterDelivery updated = deliveryRepository.save(delivery);

        log.info("Delivery cancellation completed: ID={}", deliveryId);
        return updated;
    }

    /**
     * 발송 재시도
     */
    public NewsletterDelivery retryDelivery(Long deliveryId) {
        log.info("Retrying delivery: ID={}", deliveryId);

        NewsletterDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new EntityNotFoundException("Delivery not found: " + deliveryId));

        if (delivery.getStatus() != DeliveryStatus.FAILED) {
            throw new IllegalStateException("Only failed deliveries can be retried.");
        }

        delivery.updateStatus(DeliveryStatus.PENDING);
        delivery.incrementRetryCount();
        NewsletterDelivery updated = deliveryRepository.save(delivery);

        // 재발송 수행
        performDelivery(updated);

        return updated;
    }

    /**
     * 발송 통계 조회
     */
    @Transactional(readOnly = true)
    public DeliveryStats getDeliveryStats(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching delivery statistics: {} ~ {}", startDate, endDate);

        long totalSent = deliveryRepository.countByStatusAndCreatedAtBetween(
                DeliveryStatus.SENT, startDate, endDate);
        long totalFailed = deliveryRepository.countByStatusAndCreatedAtBetween(
                DeliveryStatus.FAILED, startDate, endDate);
        long totalScheduled = deliveryRepository.countByStatusAndCreatedAtBetween(
                DeliveryStatus.PENDING, startDate, endDate);

        return DeliveryStats.builder()
                .totalSent(totalSent)
                .totalFailed(totalFailed)
                .totalScheduled(totalScheduled)
                .successRate(totalSent + totalFailed > 0 ?
                        (double) totalSent / (totalSent + totalFailed) * 100 : 0.0)
                .build();
    }

    /**
     * 뉴스레터별 발송 기록 조회
     */
    @Transactional(readOnly = true)
    public List<NewsletterDelivery> getDeliveriesByNewsletter(Long newsletterId) {
        log.info("Fetching delivery history by newsletter: newsletterId={}", newsletterId);
        return deliveryRepository.findByNewsletterIdOrderByCreatedAtDesc(newsletterId);
    }

    /**
     * 특정 기간 발송 기록 조회
     */
    @Transactional(readOnly = true)
    public List<NewsletterDelivery> getDeliveriesByPeriod(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("Fetching deliveries by period: {} ~ {}", startDate, endDate);
        return deliveryRepository.findBySentAtBetween(startDate, endDate);
    }

    /**
     * 사용자별 열람률 계산
     */
    @Transactional(readOnly = true)
    public Long countOpenedByUserId(Long userId) {
        return deliveryRepository.countOpenedByUserId(userId);
    }

    /**
     * 뉴스레터 성과 통계 조회
     */
    @Transactional(readOnly = true)
    public List<Object[]> getNewsletterPerformanceStats() {
        return deliveryRepository.getNewsletterPerformanceStats();
    }

    /**
     * 최근 실패 건수 조회
     */
    @Transactional(readOnly = true)
    public Long countFailedDeliveriesAfter(LocalDateTime date) {
        return deliveryRepository.countFailedDeliveriesAfter(date);
    }

    /**
     * 발송 상세 조회
     */
    @Transactional(readOnly = true)
    public NewsletterDelivery getDeliveryDetail(Long deliveryId) {
        return deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new EntityNotFoundException("Delivery not found: " + deliveryId));
    }

    /**
     * 실제 발송 수행 (내부 메소드)
     */
    private void performDelivery(NewsletterDelivery delivery) {
        try {
            log.info("Performing newsletter delivery: ID={}, Method={}",
                    delivery.getId(), delivery.getDeliveryMethod());

            // 발송 방법에 따른 처리
            switch (delivery.getDeliveryMethod()) {
                case EMAIL -> sendByEmail(delivery);
                case SMS -> sendBySms(delivery);
                case PUSH -> sendByPushNotification(delivery);
            }

            delivery.updateStatus(DeliveryStatus.SENT);
            delivery.setSentAt(LocalDateTime.now());

        } catch (Exception e) {
            log.error("Newsletter delivery failed: ID={}, Error={}", delivery.getId(), e.getMessage());
            delivery.updateStatus(DeliveryStatus.FAILED);
            delivery.setErrorMessage(e.getMessage());
        }

        deliveryRepository.save(delivery);
    }

    private void sendByEmail(NewsletterDelivery delivery) {
        try {
            log.info("Sending email newsletter: userId={}, newsletterId={}", 
                    delivery.getUserId(), delivery.getNewsletterId());
            
            // 새로운 구조를 사용하여 개인화된 콘텐츠 생성
            NewsletterContent content = contentService.buildPersonalizedContent(
                delivery.getUserId(), delivery.getNewsletterId());
            
            // 이메일용 HTML 렌더링
            String htmlContent = emailRenderer.renderToHtml(content);
            
            // TODO: 실제 이메일 발송 서비스 호출
            // emailService.sendHtmlEmail(delivery.getUserId(), content.getTitle(), htmlContent);
            
            log.info("Email newsletter content generated successfully for user: {}", delivery.getUserId());
            
        } catch (Exception e) {
            log.error("Failed to generate email content for delivery: {}", delivery.getId(), e);
            throw new RuntimeException("이메일 콘텐츠 생성 실패", e);
        }
    }

    private void sendBySms(NewsletterDelivery delivery) {
        // 실제 SMS 발송 로직
        log.info("Sending SMS: {}", delivery.getDeliveryMethod());
        // SmsService 호출
    }

    private void sendByPushNotification(NewsletterDelivery delivery) {
        // 실제 푸시 알림 발송 로직
        log.info("Sending push notification: {}", delivery.getDeliveryMethod());
        // PushNotificationService 호출
    }
}