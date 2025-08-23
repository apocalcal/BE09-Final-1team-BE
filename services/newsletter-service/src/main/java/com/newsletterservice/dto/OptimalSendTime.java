package com.newsletterservice.dto;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptimalSendTime {
    private List<Integer> recommendedHours;
    private List<HourlyPerformance> hourlyPerformances;
    private int analysisPeriod;
    private double confidence;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HourlyPerformance {
        private int hour;
        private long totalSent;
        private long totalOpened;
        private double openRate;
    }
}
