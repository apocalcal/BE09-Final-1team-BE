package com.newsletterservice.service;

import com.newsletterservice.entity.Subscription;
import com.newsletterservice.entity.SubscriptionStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SubscriptionStatusService {

    // === 상태 변경 처리 ===

    /**
     * 구독 상태 변경 (메인 메서드)
     */
    public void changeStatus(Subscription subscription, SubscriptionStatus newStatus) {
        SubscriptionStatus currentStatus = subscription.getStatus();

        // 1. 상태 전환 검증
        validateTransition(currentStatus, newStatus);

        // 2. 상태 변경
        subscription.setStatus(newStatus);

        // 3. 상태별 후속 처리
        handleStatusChange(subscription, currentStatus, newStatus);

        log.info("Subscription status change completed - user: {}, {} -> {}",
                subscription.getUserId(),
                currentStatus.getDescription(),
                newStatus.getDescription());
    }

    /**
     * 상태 전환 검증
     */
    public void validateTransition(SubscriptionStatus currentStatus, SubscriptionStatus newStatus) {
        if (!currentStatus.canTransitionTo(newStatus)) {
            throw new IllegalStateException(
                    String.format("Cannot transition from %s to %s status.",
                            currentStatus.getDescription(), newStatus.getDescription()));
        }
    }

    /**
     * 상태 변경에 따른 후속 처리
     */
    private void handleStatusChange(Subscription subscription,
                                    SubscriptionStatus oldStatus,
                                    SubscriptionStatus newStatus) {
        switch (newStatus) {
            case PAUSED -> handlePause(subscription);
            case UNSUBSCRIBED -> handleUnsubscribe(subscription);
            case ACTIVE -> handleReactivate(subscription, oldStatus);
        }
    }

    private void handlePause(Subscription subscription) {
        log.info("Subscription pause processing: {}", subscription.getUserId());
        // 필요시 추가 로직 (예: 일시정지 알림 발송)
    }

    private void handleUnsubscribe(Subscription subscription) {
        subscription.setUnsubscribedAt(LocalDateTime.now());
        log.info("Subscription cancellation processing: {}", subscription.getUserId());
        // 필요시 추가 로직 (예: 이탈 설문 발송, 데이터 백업)
    }

    private void handleReactivate(Subscription subscription, SubscriptionStatus oldStatus) {
        if (oldStatus == SubscriptionStatus.UNSUBSCRIBED) {
            subscription.setUnsubscribedAt(null);
            subscription.setSubscribedAt(LocalDateTime.now());
        }
        log.info("Subscription reactivation processing: {}", subscription.getUserId());
        // 필요시 추가 로직 (예: 환영 메시지 발송)
    }

    // === 유틸리티 메서드들 ===

    /**
     * 발송 가능한 상태들 조회
     */
    public Set<SubscriptionStatus> getDeliveryEligibleStatuses() {
        return Arrays.stream(SubscriptionStatus.values())
                .filter(SubscriptionStatus::canReceiveNewsletter)
                .collect(Collectors.toSet());
    }

    /**
     * 관리 가능한 상태들 조회
     */
    public Set<SubscriptionStatus> getManageableStatuses() {
        return Arrays.stream(SubscriptionStatus.values())
                .filter(SubscriptionStatus::canModifySettings)
                .collect(Collectors.toSet());
    }

    /**
     * 문자열로 상태 찾기
     */
    public SubscriptionStatus fromString(String status) {
        return Arrays.stream(SubscriptionStatus.values())
                .filter(s -> s.name().equalsIgnoreCase(status) ||
                        s.getDescription().equals(status))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid subscription status: " + status));
    }

    /**
     * 구독자 발송 자격 확인
     */
    public boolean isEligibleForDelivery(Subscription subscription) {
        return subscription.getStatus().canReceiveNewsletter();
    }
}


