package com.newsletterservice.service;

import com.newsletterservice.entity.NewsletterDelivery;
import com.newsletterservice.entity.DeliveryStatus;
import com.newsletterservice.entity.SubscriptionFrequency;
import com.newsletterservice.repository.NewsletterDeliveryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NewsletterSchedulerService {

    private final NewsletterDeliveryRepository deliveryRepository;
    private final NewsletterDeliveryService deliveryService;

    /**
     * 예약된 뉴스레터 발송 처리 (매분 실행)
     */
    @Scheduled(fixedRate = 60000) // 1분마다 실행
    @Transactional
    public void processScheduledDeliveries() {
        log.info("예약된 뉴스레터 발송 처리 시작");

        LocalDateTime now = LocalDateTime.now();
        List<NewsletterDelivery> scheduledDeliveries =
                deliveryRepository.findByStatusAndScheduledAtBefore(
                        DeliveryStatus.PENDING, now);

        if (scheduledDeliveries.isEmpty()) {
            log.debug("처리할 예약된 뉴스레터가 없습니다.");
            return;
        }

        log.info("처리할 예약된 뉴스레터 수: {}", scheduledDeliveries.size());

        for (NewsletterDelivery delivery : scheduledDeliveries) {
            try {
                processScheduledDelivery(delivery);
            } catch (Exception e) {
                log.error("예약된 뉴스레터 처리 실패: ID={}, Error={}",
                        delivery.getId(), e.getMessage());

                delivery.updateStatus(DeliveryStatus.FAILED);
                delivery.setErrorMessage(e.getMessage());
                deliveryRepository.save(delivery);
            }
        }

        log.info("예약된 뉴스레터 발송 처리 완료");
    }

    /**
     * 주기적 뉴스레터 생성 (매일 자정 실행)
     */
    @Scheduled(cron = "0 0 0 * * *") // 매일 자정
    @Transactional
    public void generateRecurringNewsletters() {
        log.info("주기적 뉴스레터 생성 시작");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime todayStart = now.truncatedTo(ChronoUnit.DAYS);

        // 일간 뉴스레터 처리
        processDailyNewsletters(todayStart);

        // 주간 뉴스레터 처리 (월요일)
        if (now.getDayOfWeek().getValue() == 1) {
            processWeeklyNewsletters(todayStart);
        }

        // 월간 뉴스레터 처리 (매월 1일)
        if (now.getDayOfMonth() == 1) {
            processMonthlyNewsletters(todayStart);
        }

        log.info("주기적 뉴스레터 생성 완료");
    }

    /**
     * 실패한 발송 재시도 (매 30분마다 실행)
     */
    @Scheduled(fixedRate = 1800000) // 30분마다
    @Transactional
    public void retryFailedDeliveries() {
        log.info("실패한 발송 재시도 처리 시작");

        LocalDateTime cutoff = LocalDateTime.now().minusHours(24); // 24시간 이내 실패건
        List<NewsletterDelivery> failedDeliveries =
                deliveryRepository.findByStatusAndUpdatedAtAfterAndRetryCountLessThan(
                        DeliveryStatus.FAILED, cutoff, 3); // 최대 3회 재시도

        if (failedDeliveries.isEmpty()) {
            log.debug("재시도할 실패한 발송이 없습니다.");
            return;
        }

        log.info("재시도할 실패한 발송 수: {}", failedDeliveries.size());

        for (NewsletterDelivery delivery : failedDeliveries) {
            try {
                retryFailedDelivery(delivery);
            } catch (Exception e) {
                log.error("실패한 발송 재시도 중 오류: ID={}, Error={}",
                        delivery.getId(), e.getMessage());
            }
        }

        log.info("실패한 발송 재시도 처리 완료");
    }

    /**
     * 오래된 발송 기록 정리 (매주 일요일 새벽 2시)
     */
    @Scheduled(cron = "0 0 2 * * SUN")
    @Transactional
    public void cleanupOldDeliveries() {
        log.info("오래된 발송 기록 정리 시작");

        LocalDateTime cutoff = LocalDateTime.now().minusDays(90); // 90일 이전

        List<NewsletterDelivery> oldDeliveries =
                deliveryRepository.findByCreatedAtBeforeAndStatusIn(
                        cutoff, List.of(DeliveryStatus.SENT, DeliveryStatus.BOUNCED));

        if (oldDeliveries.isEmpty()) {
            log.info("정리할 오래된 기록이 없습니다.");
            return;
        }

        log.info("정리할 오래된 기록 수: {}", oldDeliveries.size());

        // 실제로는 삭제하지 않고 아카이브 처리하거나 별도 테이블로 이관
        // 여기서는 로그만 출력
        oldDeliveries.forEach(delivery -> {
            log.debug("아카이브 대상: ID={}, 생성일={}",
                    delivery.getId(), delivery.getCreatedAt());
        });

        log.info("오래된 발송 기록 정리 완료");
    }

    @Async
    @Transactional
    private void processScheduledDelivery(NewsletterDelivery delivery) {
        log.info("예약된 뉴스레터 처리: ID={}", delivery.getId());

        delivery.updateStatus(DeliveryStatus.PROCESSING);
        deliveryRepository.save(delivery);

        // 발송 수행
        performActualDelivery(delivery);

        // 주기적 뉴스레터인 경우 다음 발송 예약
        if (isRecurringDelivery(delivery)) {
            scheduleNextDelivery(delivery);
        }
    }

    private void processDailyNewsletters(LocalDateTime todayStart) {
        log.info("일간 뉴스레터 생성");

        // 일간 구독자들을 위한 뉴스레터 생성
        List<Long> dailySubscribers = getDailySubscribers();

        for (Long subscriberId : dailySubscribers) {
            createDailyNewsletter(subscriberId, todayStart.plusHours(9)); // 오전 9시 발송
        }
    }

    private void processWeeklyNewsletters(LocalDateTime weekStart) {
        log.info("주간 뉴스레터 생성");

        List<Long> weeklySubscribers = getWeeklySubscribers();

        for (Long subscriberId : weeklySubscribers) {
            createWeeklyNewsletter(subscriberId, weekStart.plusHours(10)); // 오전 10시 발송
        }
    }

    private void processMonthlyNewsletters(LocalDateTime monthStart) {
        log.info("월간 뉴스레터 생성");

        List<Long> monthlySubscribers = getMonthlySubscribers();

        for (Long subscriberId : monthlySubscribers) {
            createMonthlyNewsletter(subscriberId, monthStart.plusHours(11)); // 오전 11시 발송
        }
    }

    private void retryFailedDelivery(NewsletterDelivery delivery) {
        log.info("실패한 발송 재시도: ID={}", delivery.getId());

        delivery.updateStatus(DeliveryStatus.PROCESSING);
        delivery.incrementRetryCount();
        deliveryRepository.save(delivery);

        // 재발송 수행
        performActualDelivery(delivery);
    }

    private void performActualDelivery(NewsletterDelivery delivery) {
        try {
            // 실제 발송 로직 (이메일, SMS, 푸시 등)
            switch (delivery.getDeliveryMethod()) {
                case EMAIL -> sendEmail(delivery);
                case SMS -> sendSms(delivery);
                case PUSH -> sendPushNotification(delivery);
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

    private boolean isRecurringDelivery(NewsletterDelivery delivery) {
        // NewsletterDelivery 엔티티에는 subscriptionFrequency 필드가 없으므로
        // scheduledAt이 설정되어 있는지로 판단
        return delivery.getScheduledAt() != null;
    }

    private void scheduleNextDelivery(NewsletterDelivery originalDelivery) {
        LocalDateTime nextScheduledTime = calculateNextDeliveryTime(
                originalDelivery.getScheduledAt(),
                SubscriptionFrequency.DAILY // 기본값으로 DAILY 사용
        );

        NewsletterDelivery nextDelivery = NewsletterDelivery.builder()
                .newsletterId(originalDelivery.getNewsletterId())
                .userId(originalDelivery.getUserId())
                .personalizedContent(originalDelivery.getPersonalizedContent())
                .deliveryMethod(originalDelivery.getDeliveryMethod())
                .scheduledAt(nextScheduledTime)
                .status(DeliveryStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        deliveryRepository.save(nextDelivery);

        log.info("다음 뉴스레터 예약 완료: 원본ID={}, 새ID={}, 예약시간={}",
                originalDelivery.getId(), nextDelivery.getId(), nextScheduledTime);
    }

    private LocalDateTime calculateNextDeliveryTime(LocalDateTime current, SubscriptionFrequency frequency) {
        return switch (frequency) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case MONTHLY -> current.plusMonths(1);
        };
    }

    // 구독자 조회 메소드들 (실제로는 별도 구독자 관리 서비스에서 조회)
    private List<Long> getDailySubscribers() {
        // SubscriberService에서 일간 구독자 목록 조회
        return List.of(); // 임시
    }

    private List<Long> getWeeklySubscribers() {
        // SubscriberService에서 주간 구독자 목록 조회
        return List.of(); // 임시
    }

    private List<Long> getMonthlySubscribers() {
        // SubscriberService에서 월간 구독자 목록 조회
        return List.of(); // 임시
    }

    private void createDailyNewsletter(Long subscriberId, LocalDateTime scheduledTime) {
        // 일간 뉴스레터 템플릿으로 생성
        log.info("일간 뉴스레터 생성: {}, 예약시간: {}", subscriberId, scheduledTime);
    }

    private void createWeeklyNewsletter(Long subscriberId, LocalDateTime scheduledTime) {
        // 주간 뉴스레터 템플릿으로 생성
        log.info("주간 뉴스레터 생성: {}, 예약시간: {}", subscriberId, scheduledTime);
    }

    private void createMonthlyNewsletter(Long subscriberId, LocalDateTime scheduledTime) {
        // 월간 뉴스레터 템플릿으로 생성
        log.info("월간 뉴스레터 생성: {}, 예약시간: {}", subscriberId, scheduledTime);
    }

    private void sendEmail(NewsletterDelivery delivery) {
        // 실제 이메일 발송
        log.info("이메일 발송: userId={}", delivery.getUserId());
    }

    private void sendSms(NewsletterDelivery delivery) {
        // 실제 SMS 발송
        log.info("SMS 발송: userId={}", delivery.getUserId());
    }

    private void sendPushNotification(NewsletterDelivery delivery) {
        // 실제 푸시 알림 발송
        log.info("푸시 알림 발송: userId={}", delivery.getUserId());
    }
}