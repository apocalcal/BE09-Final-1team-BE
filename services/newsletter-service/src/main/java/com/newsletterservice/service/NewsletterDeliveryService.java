package com.newsletterservice.service;


import com.newsletterservice.entity.NewsletterDelivery;
import com.newsletterservice.entity.DeliveryStatus;
import com.newsletterservice.entity.DeliveryMethod;
import com.newsletterservice.entity.SubscriptionFrequency;
import com.newsletterservice.repository.NewsletterDeliveryRepository;
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
public class NewsletterDeliveryService {

    private final NewsletterDeliveryRepository deliveryRepository;

    /**
     * 뉴스레터 발송 예약
     */
    public NewsletterDelivery scheduleDelivery(NewsletterDelivery requestDTO) {
        log.info("뉴스레터 발송 예약 시작: {}", requestDTO.getId());

        NewsletterDelivery delivery = NewsletterDelivery.builder()
                .newsletterId(requestDTO.getNewsletterId())
                .userId(requestDTO.getUserId())
                .personalizedContent(requestDTO.getPersonalizedContent())
                .recipientEmail(requestDTO.getRecipientEmail())
                .senderEmail(requestDTO.getSenderEmail())
                .deliveryMethod(requestDTO.getDeliveryMethod())
                .subscriptionFrequency(requestDTO.getSubscriptionFrequency())
                .scheduledAt(requestDTO.getScheduledAt())
                .status(DeliveryStatus.SCHEDULED)
                .createdAt(LocalDateTime.now())
                .build();

        NewsletterDelivery saved = deliveryRepository.save(delivery);
        log.info("뉴스레터 발송 예약 완료: ID={}", saved.getId());

        return convertToResponseDTO(saved);
    }

    /**
     * 즉시 발송
     */
    public NewsletterDelivery sendImmediately(NewsletterDelivery requestDTO) {
        log.info("즉시 뉴스레터 발송 시작: {}", requestDTO.getTitle());

        NewsletterDelivery delivery = NewsletterDelivery.builder()
                .title(requestDTO.getTitle())
                .content(requestDTO.getContent())
                .recipientEmail(requestDTO.getRecipientEmail())
                .senderEmail(requestDTO.getSenderEmail())
                .deliveryMethod(requestDTO.getDeliveryMethod())
                .subscriptionFrequency(SubscriptionFrequency.IMMEDIATE)
                .scheduledAt(LocalDateTime.now())
                .status(DeliveryStatus.IN_PROGRESS)
                .createdAt(LocalDateTime.now())
                .build();

        NewsletterDelivery saved = deliveryRepository.save(delivery);

        // 실제 발송 로직 수행
        performDelivery(saved);

        return convertToResponseDTO(saved);
    }

    /**
     * 발송 상태별 조회
     */
    @Transactional(readOnly = true)
    public Page<NewsletterDelivery> getDeliveriesByStatus(
            DeliveryStatus status, Pageable pageable) {

        log.info("발송 상태별 조회: status={}", status);

        Page<NewsletterDelivery> deliveries = deliveryRepository.findByStatus(status, pageable);
        return deliveries.map(this::convertToResponseDTO);
    }

    /**
     * 수신자별 발송 이력 조회
     */
    @Transactional(readOnly = true)
    public Page<NewsletterDelivery> getDeliveriesByRecipient(
            String recipientEmail, Pageable pageable) {

        log.info("수신자별 발송 이력 조회: email={}", recipientEmail);

        Page<NewsletterDelivery> deliveries =
                deliveryRepository.findByRecipientEmailOrderByCreatedAtDesc(recipientEmail, pageable);
        return deliveries.map(this::convertToResponseDTO);
    }

    /**
     * 예약된 발송 목록 조회
     */
    @Transactional(readOnly = true)
    public List<NewsletterDelivery> getScheduledDeliveries() {
        log.info("예약된 발송 목록 조회");

        List<NewsletterDelivery> scheduled =
                deliveryRepository.findByStatusAndScheduledAtBefore(
                        DeliveryStatus.SCHEDULED, LocalDateTime.now());

        return scheduled.stream()
                .map(this::convertToResponseDTO)
                .toList();
    }

    /**
     * 발송 취소
     */
    public NewsletterDelivery cancelDelivery(Long deliveryId) {
        log.info("발송 취소: ID={}", deliveryId);

        NewsletterDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new EntityNotFoundException("발송 정보를 찾을 수 없습니다: " + deliveryId));

        if (delivery.getStatus() == DeliveryStatus.SENT) {
            throw new IllegalStateException("이미 발송된 뉴스레터는 취소할 수 없습니다.");
        }

        delivery.updateStatus(DeliveryStatus.CANCELLED);
        NewsletterDelivery updated = deliveryRepository.save(delivery);

        log.info("발송 취소 완료: ID={}", deliveryId);
        return convertToResponseDTO(updated);
    }

    /**
     * 발송 재시도
     */
    public NewsletterDelivery retryDelivery(Long deliveryId) {
        log.info("발송 재시도: ID={}", deliveryId);

        NewsletterDelivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new EntityNotFoundException("발송 정보를 찾을 수 없습니다: " + deliveryId));

        if (delivery.getStatus() != DeliveryStatus.FAILED) {
            throw new IllegalStateException("실패한 발송만 재시도할 수 있습니다.");
        }

        delivery.updateStatus(DeliveryStatus.IN_PROGRESS);
        delivery.incrementRetryCount();
        NewsletterDelivery updated = deliveryRepository.save(delivery);

        // 재발송 수행
        performDelivery(updated);

        return convertToResponseDTO(updated);
    }

    /**
     * 발송 통계 조회
     */
    @Transactional(readOnly = true)
    public DeliveryStatsDTO getDeliveryStats(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("발송 통계 조회: {} ~ {}", startDate, endDate);

        long totalSent = deliveryRepository.countByStatusAndCreatedAtBetween(
                DeliveryStatus.SENT, startDate, endDate);
        long totalFailed = deliveryRepository.countByStatusAndCreatedAtBetween(
                DeliveryStatus.FAILED, startDate, endDate);
        long totalScheduled = deliveryRepository.countByStatusAndCreatedAtBetween(
                DeliveryStatus.SCHEDULED, startDate, endDate);

        return DeliveryStatsDTO.builder()
                .totalSent(totalSent)
                .totalFailed(totalFailed)
                .totalScheduled(totalScheduled)
                .successRate(totalSent + totalFailed > 0 ?
                        (double) totalSent / (totalSent + totalFailed) * 100 : 0.0)
                .build();
    }

    /**
     * 실제 발송 수행 (내부 메소드)
     */
    private void performDelivery(NewsletterDelivery delivery) {
        try {
            log.info("뉴스레터 발송 수행: ID={}, Method={}",
                    delivery.getId(), delivery.getDeliveryMethod());

            // 발송 방법에 따른 처리
            switch (delivery.getDeliveryMethod()) {
                case EMAIL -> sendByEmail(delivery);
                case SMS -> sendBySms(delivery);
                case PUSH_NOTIFICATION -> sendByPushNotification(delivery);
            }

            delivery.updateStatus(DeliveryStatus.SENT);
            delivery.setSentAt(LocalDateTime.now());

        } catch (Exception e) {
            log.error("뉴스레터 발송 실패: ID={}, Error={}", delivery.getId(), e.getMessage());
            delivery.updateStatus(DeliveryStatus.FAILED);
            delivery.setErrorMessage(e.getMessage());
        }

        deliveryRepository.save(delivery);
    }

    private void sendByEmail(NewsletterDelivery delivery) {
        // 실제 이메일 발송 로직
        log.info("이메일 발송: {} -> {}", delivery.getSenderEmail(), delivery.getRecipientEmail());
        // EmailService 호출
    }

    private void sendBySms(NewsletterDelivery delivery) {
        // 실제 SMS 발송 로직
        log.info("SMS 발송: {}", delivery.getRecipientEmail());
        // SmsService 호출
    }

    private void sendByPushNotification(NewsletterDelivery delivery) {
        // 실제 푸시 알림 발송 로직
        log.info("푸시 알림 발송: {}", delivery.getRecipientEmail());
        // PushNotificationService 호출
    }

    private NewsletterDelivery convertToResponseDTO(NewsletterDelivery delivery) {
        return NewsletterDelivery.builder()
                .id(delivery.getId())
                .title(delivery.getTitle())
                .content(delivery.getContent())
                .recipientEmail(delivery.getRecipientEmail())
                .senderEmail(delivery.getSenderEmail())
                .deliveryMethod(delivery.getDeliveryMethod())
                .subscriptionFrequency(delivery.getSubscriptionFrequency())
                .status(delivery.getStatus())
                .scheduledAt(delivery.getScheduledAt())
                .sentAt(delivery.getSentAt())
                .createdAt(delivery.getCreatedAt())
                .updatedAt(delivery.getUpdatedAt())
                .retryCount(delivery.getRetryCount())
                .errorMessage(delivery.getErrorMessage())
                .build();
    }

    // 통계 DTO
    public static class DeliveryStatsDTO {
        private final long totalSent;
        private final long totalFailed;
        private final long totalScheduled;
        private final double successRate;

        @lombok.Builder
        public DeliveryStatsDTO(long totalSent, long totalFailed, long totalScheduled, double successRate) {
            this.totalSent = totalSent;
            this.totalFailed = totalFailed;
            this.totalScheduled = totalScheduled;
            this.successRate = successRate;
        }

        // Getters
        public long getTotalSent() { return totalSent; }
        public long getTotalFailed() { return totalFailed; }
        public long getTotalScheduled() { return totalScheduled; }
        public double getSuccessRate() { return successRate; }
    }
}