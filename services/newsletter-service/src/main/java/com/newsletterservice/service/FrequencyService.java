package com.newsletterservice.service;

import com.newsletterservice.entity.SubscriptionFrequency;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;


@Service
@Component
public class FrequencyService {
    //  비즈니스 로직
    public LocalDateTime getNextDeliveryTime(SubscriptionFrequency frequency,
                                             LocalDateTime lastSent,
                                             int preferredHour) {
        return switch (frequency) {
            case DAILY -> lastSent.plusDays(1)
                    .withHour(preferredHour).withMinute(0).withSecond(0).withNano(0);
            case WEEKLY -> lastSent.with(TemporalAdjusters.next(DayOfWeek.MONDAY))
                    .withHour(preferredHour).withMinute(0).withSecond(0).withNano(0);
            case MONTHLY -> lastSent.with(TemporalAdjusters.firstDayOfNextMonth())
                    .withHour(preferredHour).withMinute(0).withSecond(0).withNano(0);
        };
    }

    public boolean shouldSendToday(SubscriptionFrequency frequency,
                                   LocalDateTime lastSent,
                                   LocalDateTime now) {
        if (lastSent == null) return true;

        return switch (frequency) {
            case DAILY -> lastSent.toLocalDate().isBefore(now.toLocalDate());
            case WEEKLY -> ChronoUnit.DAYS.between(lastSent.toLocalDate(), now.toLocalDate()) >= 7;
            case MONTHLY -> ChronoUnit.MONTHS.between(lastSent.toLocalDate(), now.toLocalDate()) >= 1;
        };
    }

    public int getExpectedNewsCount(SubscriptionFrequency frequency) {
        return switch (frequency) {
            case DAILY -> 5;
            case WEEKLY -> 10;
            case MONTHLY -> 20;
        };
    }
}

