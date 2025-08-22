package com.newsletterservice.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SubscriptionFrequency {
    DAILY("매일"),
    WEEKLY("주간"),
    MONTHLY("월간");

    private final String description;
}