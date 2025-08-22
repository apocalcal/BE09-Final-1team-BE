package com.newsletterservice.dto;

import com.newsletterservice.entity.DeliveryStatus;
import com.newsletterservice.entity.DeliveryMethod;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterDeliveryResponse {
    private Long id;
    private Long newsletterId;
    private Long userId;
    private String title;
    private DeliveryStatus status;
    private DeliveryMethod deliveryMethod;
    private LocalDateTime sentAt;
    private LocalDateTime openedAt;
    private LocalDateTime createdAt;
}