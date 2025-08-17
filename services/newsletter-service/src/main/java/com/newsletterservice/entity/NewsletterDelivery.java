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
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}