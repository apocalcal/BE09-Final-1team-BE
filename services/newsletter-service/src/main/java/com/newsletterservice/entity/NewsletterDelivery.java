package com.newsletterservice.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "newsletter_delivery")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsletterDelivery {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "newsletter_id", nullable = false)
    private Long newsletterId;
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "personalized_content", columnDefinition = "TEXT")
    private String personalizedContent; // 개인화된 뉴스레터 내용

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "opened_at")
    private LocalDateTime openedAt; // 열람 시간

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private DeliveryStatus status;

    @Column(name = "delivery_method")
    @Enumerated(EnumType.STRING)
    private DeliveryMethod deliveryMethod; // EMAIL, PUSH, SMS

    @Column(name = "scheduled_at")
    private LocalDateTime scheduledAt; // 예약 발송 시간

    @Column(name = "retry_count", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer retryCount = 0; // 재시도 횟수

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage; // 에러 메시지

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = DeliveryStatus.PENDING;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // 상태 업데이트 메서드
    public void updateStatus(DeliveryStatus newStatus) {
        this.status = newStatus;
        this.updatedAt = LocalDateTime.now();
    }

    // 재시도 횟수 증가 메서드
    public void incrementRetryCount() {
        this.retryCount = (this.retryCount == null) ? 1 : this.retryCount + 1;
        this.updatedAt = LocalDateTime.now();
    }

    // 에러 메시지 설정 메서드
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        this.updatedAt = LocalDateTime.now();
    }
}