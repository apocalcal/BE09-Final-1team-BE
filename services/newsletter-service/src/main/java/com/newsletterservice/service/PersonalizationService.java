package com.newsletterservice.service;

import com.newsletterservice.client.NewsServiceClient;
import com.newsletterservice.client.UserServiceClient;
import com.newsletterservice.client.dto.NewsResponse;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.entity.Subscription;
import com.newsletterservice.repository.SubscriptionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalizationService {

    private final NewsServiceClient newsServiceClient;
    private final UserServiceClient userServiceClient;
    private final SubscriptionRepository subscriptionRepository;
    private final UserBehaviorTrackingService behaviorTrackingService;
    private final ObjectMapper objectMapper;

    /**
     * 사용자별 개인화된 뉴스 추천
     */
    public List<NewsResponse> getPersonalizedNews(Long userId, int limit) {
        log.info("Getting personalized news for user: {}", userId);

        // 1. 사용자 구독 정보 조회
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUserId(userId);

        if (subscriptionOpt.isEmpty() || !subscriptionOpt.get().isPersonalized()) {
            log.info("User {} has no personalized subscription, returning default news", userId);
            return getDefaultNews(limit);
        }

        Subscription subscription = subscriptionOpt.get();

        // 2. 개인화 점수 계산
        Map<NewsResponse, Double> newsScores = new HashMap<>();

        // 2-1. 관심사 카테고리 기반 뉴스 가져오기
        List<String> preferredCategories = parseJsonToList(subscription.getPreferredCategories());
        if (!preferredCategories.isEmpty()) {
            for (String category : preferredCategories) {
                try {
                    ApiResponse<List<NewsResponse>> categoryNewsResponse =
                            newsServiceClient.getNewsByCategory(category, 0, 15);
                    List<NewsResponse> categoryNews = categoryNewsResponse.getData();

                    if (categoryNews != null) {
                        for (NewsResponse news : categoryNews) {
                            double score = calculatePersonalizationScore(news, subscription, userId);
                            newsScores.put(news, score);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to get news for category: {}", category, e);
                }
            }
        }

        // 2-2. 트렌딩 뉴스도 일부 포함 (다양성 확보)
        try {
            ApiResponse<List<NewsResponse>> trendingNewsResponse =
                    newsServiceClient.getTrendingNews(24, 10);
            List<NewsResponse> trendingNews = trendingNewsResponse.getData();

            if (trendingNews != null) {
                for (NewsResponse news : trendingNews) {
                    if (!newsScores.containsKey(news)) {
                        double score = calculatePersonalizationScore(news, subscription, userId) * 0.8; // 가중치 적용
                        newsScores.put(news, score);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get trending news", e);
        }

        // 2-3. 최신 뉴스도 추가 (다양성 확보)
        try {
            ApiResponse<List<NewsResponse>> latestNewsResponse =
                    newsServiceClient.getLatestNews(null, 10);
            List<NewsResponse> latestNews = latestNewsResponse.getData();

            if (latestNews != null) {
                for (NewsResponse news : latestNews) {
                    if (!newsScores.containsKey(news)) {
                        double score = calculatePersonalizationScore(news, subscription, userId) * 0.6; // 낮은 가중치
                        newsScores.put(news, score);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to get latest news", e);
        }

        // 3. 점수순으로 정렬하여 반환
        List<NewsResponse> result = newsScores.entrySet().stream()
                .sorted(Map.Entry.<NewsResponse, Double>comparingByValue().reversed())
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        log.info("Personalized news generated for user {}: {} articles", userId, result.size());
        return result;
    }

    /**
     * 특정 뉴스의 개인화 점수 계산 (외부에서 호출 가능)
     */
    public double getPersonalizationScore(NewsResponse news, Long userId) {
        Optional<Subscription> subscriptionOpt = subscriptionRepository.findByUserId(userId);
        
        if (subscriptionOpt.isEmpty() || !subscriptionOpt.get().isPersonalized()) {
            return 0.0; // 개인화되지 않은 경우
        }
        
        return calculatePersonalizationScore(news, subscriptionOpt.get(), userId);
    }

    /**
     * 개인화 점수 계산 - 추천 알고리즘의 핵심
     */
    private double calculatePersonalizationScore(NewsResponse news, Subscription subscription, Long userId) {
        double score = 0.0;

        // 1. 카테고리 선호도 점수 (40%)
        double categoryScore = calculateCategoryScore(news, subscription, userId);
        score += categoryScore * 0.4;

        // 2. 키워드 매칭 점수 (25%)
        double keywordScore = calculateKeywordScore(news, subscription);
        score += keywordScore * 0.25;

        // 3. 뉴스 인기도 점수 (20%)
        double popularityScore = calculatePopularityScore(news);
        score += popularityScore * 0.2;

        // 4. 최신성 점수 (10%)
        double freshnessScore = calculateFreshnessScore(news);
        score += freshnessScore * 0.1;

        // 5. 사용자 행동 패턴 점수 (5%)
        double behaviorScore = calculateBehaviorScore(news, userId);
        score += behaviorScore * 0.05;

        log.debug("Score for news '{}': category={}, keyword={}, popularity={}, freshness={}, behavior={}, total={}",
                news.getTitle(), categoryScore, keywordScore, popularityScore, freshnessScore, behaviorScore, score);

        return score;
    }

    /**
     * 카테고리별 선호도 점수 계산
     * - 구독 정보의 관심 카테고리
     * - 사용자 클릭/조회 히스토리 기반 선호도
     */
    private double calculateCategoryScore(NewsResponse news, Subscription subscription, Long userId) {
        // 1. 구독 설정 기반 점수
        List<String> preferredCategories = parseJsonToList(subscription.getPreferredCategories());
        double subscriptionScore = preferredCategories.contains(news.getCategory()) ? 1.0 : 0.0;

        // 2. 사용자 행동 기반 카테고리 선호도
        Map<String, Double> categoryPreferences = behaviorTrackingService.getUserCategoryPreferences(userId);
        double behaviorScore = categoryPreferences.getOrDefault(news.getCategory(), 0.0);

        // 3. 두 점수를 결합 (구독 설정 70%, 행동 데이터 30%)
        return subscriptionScore * 0.7 + behaviorScore * 0.3;
    }

    /**
     * 키워드 매칭 점수 계산
     */
    private double calculateKeywordScore(NewsResponse news, Subscription subscription) {
        List<String> keywords = parseJsonToList(subscription.getKeywords());
        if (keywords.isEmpty()) {
            return 0.0;
        }

        String newsText = (news.getTitle() + " " +
                (news.getSummary() != null ? news.getSummary() : "")).toLowerCase();

        long matchCount = keywords.stream()
                .mapToLong(keyword ->
                        newsText.contains(keyword.toLowerCase()) ? 1 : 0)
                .sum();

        return (double) matchCount / keywords.size();
    }

    /**
     * 뉴스 인기도 점수 계산
     * - 조회수, 공유수 등을 기반으로 점수 계산
     */
    private double calculatePopularityScore(NewsResponse news) {
        // TODO: 실제로는 뉴스의 조회수, 공유수, 댓글수 등을 활용해야 함
        // 현재는 임시로 카테고리별 가중치 적용
        return switch (news.getCategory()) {
            case "POLITICS", "ECONOMY" -> 0.8; // 높은 관심도
            case "SOCIETY", "IT_SCIENCE" -> 0.7;
            case "CULTURE", "INTERNATIONAL" -> 0.6;
            default -> 0.5;
        };
    }

    /**
     * 최신성 점수 계산
     * - 시간이 지날수록 점수 감소
     */
    private double calculateFreshnessScore(NewsResponse news) {
        if (news.getCreatedAt() == null) {
            return 0.5; // 기본값
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime newsTime = news.getCreatedAt();
        long hoursAgo = ChronoUnit.HOURS.between(newsTime, now);

        // 1시간 이내: 1.0, 24시간 이내: 선형 감소, 그 이후: 0.1
        if (hoursAgo <= 1) {
            return 1.0;
        } else if (hoursAgo <= 24) {
            return Math.max(0.1, 1.0 - (hoursAgo - 1) / 23.0 * 0.9);
        } else {
            return 0.1;
        }
    }

    /**
     * 사용자 행동 패턴 점수 계산
     */
    private double calculateBehaviorScore(NewsResponse news, Long userId) {
        // 사용자가 과거에 비슷한 뉴스를 많이 클릭했는지 확인
        return behaviorTrackingService.getSimilarNewsClickRate(userId, news.getCategory());
    }

    /**
     * 기본 뉴스 반환 (개인화되지 않은 경우)
     */
    private List<NewsResponse> getDefaultNews(int limit) {
        try {
            // 트렌딩 뉴스와 최신 뉴스를 섞어서 반환
            List<NewsResponse> result = new ArrayList<>();

            // 트렌딩 뉴스 절반
            ApiResponse<List<NewsResponse>> trendingResponse =
                    newsServiceClient.getTrendingNews(24, limit / 2);
            if (trendingResponse.getData() != null) {
                result.addAll(trendingResponse.getData());
            }

            // 최신 뉴스 절반
            ApiResponse<List<NewsResponse>> latestResponse =
                    newsServiceClient.getLatestNews(null, limit - result.size());
            if (latestResponse.getData() != null) {
                result.addAll(latestResponse.getData());
            }

            return result.stream().limit(limit).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Failed to get default news", e);
            return Collections.emptyList();
        }
    }

    /**
     * JSON 문자열을 List로 파싱
     */
    private List<String> parseJsonToList(String jsonString) {
        if (jsonString == null || jsonString.trim().isEmpty() || "[]".equals(jsonString.trim())) {
            return Collections.emptyList();
        }

        try {
            return objectMapper.readValue(jsonString, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            log.warn("Failed to parse JSON: {}", jsonString, e);
            return Collections.emptyList();
        }
    }
}