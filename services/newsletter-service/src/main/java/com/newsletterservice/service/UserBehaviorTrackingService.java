package com.newsletterservice.service;

import com.newsletterservice.entity.UserNewsInteraction;
import com.newsletterservice.entity.InteractionType;
import com.newsletterservice.repository.UserNewsInteractionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserBehaviorTrackingService {

    private final UserNewsInteractionRepository interactionRepository;

    /**
     * 사용자 뉴스 조회 기록
     */
    public void trackNewsView(Long userId, Long newsId, String category) {
        try {
            UserNewsInteraction interaction = UserNewsInteraction.builder()
                    .userId(userId)
                    .newsId(newsId)
                    .category(category)
                    .type(InteractionType.VIEW)
                    .createdAt(LocalDateTime.now())
                    .build();

            interactionRepository.save(interaction);
            log.debug("Tracked news view: userId={}, newsId={}, category={}", userId, newsId, category);
        } catch (Exception e) {
            log.error("Failed to track news view", e);
        }
    }

    /**
     * 사용자 뉴스 클릭 기록
     */
    public void trackNewsClick(Long userId, Long newsId, String category) {
        try {
            UserNewsInteraction interaction = UserNewsInteraction.builder()
                    .userId(userId)
                    .newsId(newsId)
                    .category(category)
                    .type(InteractionType.CLICK)
                    .createdAt(LocalDateTime.now())
                    .build();

            interactionRepository.save(interaction);
            log.debug("Tracked news click: userId={}, newsId={}, category={}", userId, newsId, category);
        } catch (Exception e) {
            log.error("Failed to track news click", e);
        }
    }

    /**
     * 사용자 카테고리별 선호도 계산
     * 최근 30일간의 클릭 데이터를 바탕으로 선호도 점수 계산
     */
    public Map<String, Double> getUserCategoryPreferences(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // 최근 30일간 클릭 데이터 조회
        List<UserNewsInteraction> interactions = interactionRepository
                .findByUserIdAndTypeAndCreatedAtAfter(userId, InteractionType.CLICK, thirtyDaysAgo);

        if (interactions.isEmpty()) {
            return new HashMap<>();
        }

        // 카테고리별 클릭 수 계산
        Map<String, Long> categoryClickCounts = interactions.stream()
                .collect(Collectors.groupingBy(
                        UserNewsInteraction::getCategory,
                        Collectors.counting()
                ));

        // 전체 클릭 수
        long totalClicks = interactions.size();

        // 카테고리별 선호도 점수 계산 (0.0 ~ 1.0)
        Map<String, Double> preferences = new HashMap<>();
        for (Map.Entry<String, Long> entry : categoryClickCounts.entrySet()) {
            double preference = (double) entry.getValue() / totalClicks;
            preferences.put(entry.getKey(), preference);
        }

        log.debug("User {} category preferences: {}", userId, preferences);
        return preferences;
    }

    /**
     * 특정 카테고리의 뉴스에 대한 사용자의 클릭률 계산
     */
    public double getSimilarNewsClickRate(Long userId, String category) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        // 해당 카테고리 뉴스 조회 수
        long viewCount = interactionRepository.countByUserIdAndCategoryAndTypeAndCreatedAtAfter(
                userId, category, InteractionType.VIEW, thirtyDaysAgo);

        // 해당 카테고리 뉴스 클릭 수
        long clickCount = interactionRepository.countByUserIdAndCategoryAndTypeAndCreatedAtAfter(
                userId, category, InteractionType.CLICK, thirtyDaysAgo);

        if (viewCount == 0) {
            return 0.0;
        }

        double clickRate = (double) clickCount / viewCount;
        log.debug("User {} click rate for category {}: {}/{} = {}",
                userId, category, clickCount, viewCount, clickRate);

        return clickRate;
    }

    /**
     * 사용자 활동성 점수 계산
     * 최근 활동이 많을수록 높은 점수
     */
    public double getUserActivityScore(Long userId) {
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);

        long recentInteractions = interactionRepository
                .countByUserIdAndCreatedAtAfter(userId, sevenDaysAgo);

        // 일주일에 10회 이상 활동하면 1.0, 그 이하는 비례적으로 감소
        return Math.min(1.0, recentInteractions / 10.0);
    }

    /**
     * 사용자가 선호하는 뉴스 발행 시간대 분석
     */
    public Map<Integer, Long> getUserPreferredReadingHours(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<UserNewsInteraction> interactions = interactionRepository
                .findByUserIdAndTypeAndCreatedAtAfter(userId, InteractionType.CLICK, thirtyDaysAgo);

        return interactions.stream()
                .collect(Collectors.groupingBy(
                        interaction -> interaction.getCreatedAt().getHour(),
                        Collectors.counting()
                ));
    }

    /**
     * 다양성 점수 계산
     * 사용자가 다양한 카테고리의 뉴스를 읽는지 측정
     */
    public double getUserDiversityScore(Long userId) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<String> categoriesRead = interactionRepository
                .findByUserIdAndTypeAndCreatedAtAfter(userId, InteractionType.CLICK, thirtyDaysAgo)
                .stream()
                .map(UserNewsInteraction::getCategory)
                .distinct()
                .collect(Collectors.toList());

        // 전체 카테고리 수 대비 읽은 카테고리 수의 비율
        int totalCategories = 9; // NewsCategory enum의 총 개수
        return (double) categoriesRead.size() / totalCategories;
    }
}