package com.newsletterservice.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsletterPerformance {
    private Long newsletterId;
    private int totalDeliveries;
    private long successfulDeliveries;
    private long openedCount;
    private long failedCount;
    private double openRate;
    private double failureRate;
    private double avgOpenDelayMinutes;
    private int analysisPeriod;
}
