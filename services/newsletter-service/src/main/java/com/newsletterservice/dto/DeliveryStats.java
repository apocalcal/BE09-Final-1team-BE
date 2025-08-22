package com.newsletterservice.dto;

import lombok.*;

@Getter
@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
// 통계 DTO
public class DeliveryStats {
    private long totalSent;
    private long totalFailed;
    private long totalScheduled;
    private double successRate;

}
