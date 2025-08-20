package com.newsletterservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newsletterservice.dto.SubscriptionRequest;
import com.newsletterservice.dto.SubscriptionResponse;
import com.newsletterservice.entity.NewsCategory;
import com.newsletterservice.entity.Subscription;
import com.newsletterservice.entity.SubscriptionFrequency;
import com.newsletterservice.entity.SubscriptionStatus;
import com.newsletterservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ObjectMapper objectMapper;

    @Override
    public SubscriptionResponse subscribe(SubscriptionRequest request, String userId) {
        try {
            log.info("Newsletter subscription request - user: {}, categories: {}", userId, request.getPreferredCategories());
            
            // 기존 구독이 있는지 확인
            subscriptionRepository.findByUserId(Long.valueOf(userId));
            
            // 새로운 구독 생성
            Subscription subscription = Subscription.builder()
                    .userId(Long.valueOf(userId))
                    .email(request.getEmail())
                    .preferredCategories(convertCategoriesToJson(request.getPreferredCategories()))
                    .keywords(convertToJson(request.getKeywords()))
                    .frequency(request.getFrequency())
                    .sendTime(request.getSendTime() != null ? request.getSendTime() : 9)
                    .isPersonalized(request.isPersonalized())
                    .status(SubscriptionStatus.ACTIVE)
                    .build();
            
            Subscription savedSubscription = subscriptionRepository.save(subscription);
            
            log.info("Newsletter subscription successful - subscription ID: {}", savedSubscription.getId());
            
            return SubscriptionResponse.builder()
                    .id(savedSubscription.getId())
                    .userId(savedSubscription.getUserId())
                    .email(savedSubscription.getEmail())
                    .preferredCategories(request.getPreferredCategories())
                    .keywords(request.getKeywords())
                    .frequency(savedSubscription.getFrequency())
                    .status(savedSubscription.getStatus())
                    .sendTime(savedSubscription.getSendTime())
                    .isPersonalized(savedSubscription.isPersonalized())
                    .subscribedAt(savedSubscription.getSubscribedAt())
                    .lastSentAt(savedSubscription.getLastSentAt())
                    .createdAt(savedSubscription.getCreatedAt())
                    .build();
                    
        } catch (Exception e) {
            log.error("Newsletter subscription failed - user: {}", userId, e);
            throw new RuntimeException("Error occurred during newsletter subscription: " + e.getMessage());
        }
    }

    @Override
    public SubscriptionResponse getSubscription(Long id) {
        try {
            log.info("Fetching subscription information - subscription ID: {}", id);
            
            Subscription subscription = subscriptionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Subscription not found: " + id));
            
            return SubscriptionResponse.builder()
                    .id(subscription.getId())
                    .userId(subscription.getUserId())
                    .email(subscription.getEmail())
                    .preferredCategories(parseJsonToCategories(subscription.getPreferredCategories()))
                    .keywords(parseJsonToList(subscription.getKeywords()))
                    .frequency(subscription.getFrequency())
                    .status(subscription.getStatus())
                    .sendTime(subscription.getSendTime())
                    .isPersonalized(subscription.isPersonalized())
                    .subscribedAt(subscription.getSubscribedAt())
                    .lastSentAt(subscription.getLastSentAt())
                    .createdAt(subscription.getCreatedAt())
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to fetch subscription information - subscription ID: {}", id, e);
            throw new RuntimeException("Error occurred while fetching subscription information: " + e.getMessage());
        }
    }

    @Override
    public List<SubscriptionResponse> getSubscriptionsByUser(String userId) {
        try {
            log.info("Fetching user subscription list - user: {}", userId);
            
            Optional<Subscription> subscription = subscriptionRepository.findByUserId(Long.valueOf(userId));
            if (subscription.isPresent()) {
                Subscription sub = subscription.get();
                return List.of(SubscriptionResponse.builder()
                        .id(sub.getId())
                        .userId(sub.getUserId())
                        .email(sub.getEmail())
                        .preferredCategories(parseJsonToCategories(sub.getPreferredCategories()))
                        .keywords(parseJsonToList(sub.getKeywords()))
                        .frequency(sub.getFrequency())
                        .status(sub.getStatus())
                        .sendTime(sub.getSendTime())
                        .isPersonalized(sub.isPersonalized())
                        .subscribedAt(sub.getSubscribedAt())
                        .lastSentAt(sub.getLastSentAt())
                        .createdAt(sub.getCreatedAt())
                        .build());
            }
            return List.of();
                    
        } catch (Exception e) {
            log.error("Failed to fetch user subscription list - user: {}", userId, e);
            throw new RuntimeException("Error occurred while fetching user subscription list: " + e.getMessage());
        }
    }

    @Override
    public void unsubscribe(Long id) {
        try {
            log.info("Unsubscribing from newsletter - subscription ID: {}", id);
            
            Subscription subscription = subscriptionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Subscription not found: " + id));
            
            subscription.setStatus(SubscriptionStatus.UNSUBSCRIBED);
            subscriptionRepository.save(subscription);
            
            log.info("Newsletter unsubscription successful - subscription ID: {}", id);
            
        } catch (Exception e) {
            log.error("Newsletter unsubscription failed - subscription ID: {}", id, e);
            throw new RuntimeException("Error occurred during newsletter unsubscription: " + e.getMessage());
        }
    }

    @Override
    public List<SubscriptionResponse> getActiveSubscriptions() {
        try {
            log.info("Fetching active subscription list");
            
            List<Subscription> activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
            
            return activeSubscriptions.stream()
                    .map(subscription -> SubscriptionResponse.builder()
                            .id(subscription.getId())
                            .userId(subscription.getUserId())
                            .email(subscription.getEmail())
                            .preferredCategories(parseJsonToCategories(subscription.getPreferredCategories()))
                            .keywords(parseJsonToList(subscription.getKeywords()))
                            .frequency(subscription.getFrequency())
                            .status(subscription.getStatus())
                            .sendTime(subscription.getSendTime())
                            .isPersonalized(subscription.isPersonalized())
                            .subscribedAt(subscription.getSubscribedAt())
                            .lastSentAt(subscription.getLastSentAt())
                            .createdAt(subscription.getCreatedAt())
                            .build())
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Failed to fetch active subscription list", e);
            throw new RuntimeException("Error occurred while fetching active subscription list: " + e.getMessage());
        }
    }

    private String convertToJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert list to JSON", e);
            return "[]";
        }
    }

    private String convertCategoriesToJson(List<NewsCategory> categories) {
        if (categories == null || categories.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(categories);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert categories to JSON", e);
            return "[]";
        }
    }

    private List<String> parseJsonToList(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON to list: {}", json, e);
            return List.of();
        }
    }

    private List<NewsCategory> parseJsonToCategories(String json) {
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<NewsCategory>>() {});
        } catch (JsonProcessingException e) {
            log.error("Failed to parse JSON to categories: {}", json, e);
            return List.of();
        }
    }
}
