package com.newsletterservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SubscriptionStatus {
    ACTIVE("활성"),
    PAUSED("일시정지"),
    UNSUBSCRIBED("구독취소");

    private final String description;

    /**
     * 뉴스레터 수신 가능 여부 확인
     */
    public boolean canReceiveNewsletter() {
        return this == ACTIVE;
    }

    /**
     * 설정 변경 가능 여부 확인
     */
    public boolean canModifySettings() {
        return this != UNSUBSCRIBED;
    }

    /**
     * 상태 전환 가능 여부 확인
     */
    public boolean canTransitionTo(SubscriptionStatus newStatus) {
        return switch (this) {
            case ACTIVE -> newStatus == PAUSED || newStatus == UNSUBSCRIBED;
            case PAUSED -> newStatus == ACTIVE || newStatus == UNSUBSCRIBED;
            case UNSUBSCRIBED -> newStatus == ACTIVE; // 재구독 가능
        };
    }

    /**
     * 기본 상태 반환
     */
    public static SubscriptionStatus getDefault() {
        return ACTIVE;
    }
}
