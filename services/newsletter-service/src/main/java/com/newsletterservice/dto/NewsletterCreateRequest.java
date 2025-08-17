package com.newsletterservice.dto;

import com.newsletterservice.entity.DeliveryMethod;
import lombok.*;
import jakarta.validation.constraints.NotNull;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterCreateRequest {

    @NotNull(message = "뉴스레터 ID는 필수입니다")
    private Long newsletterId; // news-service의 newsletter ID

    private List<Long> targetUserIds; // 특정 사용자들에게 발송 (null이면 전체)

    private DeliveryMethod deliveryMethod = DeliveryMethod.EMAIL;

    private boolean isPersonalized = true; // 개인화 여부

    private boolean isScheduled = false; // 예약 발송 여부

    private String scheduledAt; // 예약 발송 시간 (ISO format)
}