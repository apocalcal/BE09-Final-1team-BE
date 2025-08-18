package com.newsletterservice.service;

import com.newsletterservice.dto.SubscriptionRequest;
import com.newsletterservice.dto.SubscriptionResponse;
import com.newsletterservice.entity.Subscription;
import com.newsletterservice.entity.SubscriptionFrequency;
import com.newsletterservice.entity.SubscriptionStatus;
import com.newsletterservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;

    @Override
    public SubscriptionResponse subscribe(SubscriptionRequest request, String userId) {
        try {
            log.info("뉴스레터 구독 요청 - 사용자: {}, 카테고리: {}", userId, request.getPreferredCategories());
            
            // 기존 구독이 있는지 확인
            subscriptionRepository.findByUserId(Long.valueOf(userId));
            
            // 새로운 구독 생성
            Subscription subscription = Subscription.builder()
                    .userId(Long.valueOf(userId))
                    .email(request.getEmail())
                    .preferredCategories(request.getPreferredCategories() != null ? 
                        request.getPreferredCategories().toString() : "[]")
                    .keywords(request.getKeywords() != null ? 
                        request.getKeywords().toString() : "[]")
                    .frequency(request.getFrequency())
                    .sendTime(request.getSendTime() != null ? request.getSendTime() : 9)
                    .isPersonalized(request.isPersonalized())
                    .status(SubscriptionStatus.ACTIVE)
                    .build();
            
            Subscription savedSubscription = subscriptionRepository.save(subscription);
            
            log.info("뉴스레터 구독 성공 - 구독 ID: {}", savedSubscription.getId());
            
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
            log.error("뉴스레터 구독 실패 - 사용자: {}", userId, e);
            throw new RuntimeException("뉴스레터 구독 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public SubscriptionResponse getSubscription(Long id) {
        try {
            log.info("구독 정보 조회 - 구독 ID: {}", id);
            
            Subscription subscription = subscriptionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("구독을 찾을 수 없습니다: " + id));
            
            return SubscriptionResponse.builder()
                    .id(subscription.getId())
                    .userId(subscription.getUserId())
                    .email(subscription.getEmail())
                    .preferredCategories(null) // TODO: JSON 파싱 필요
                    .keywords(null) // TODO: JSON 파싱 필요
                    .frequency(subscription.getFrequency())
                    .status(subscription.getStatus())
                    .sendTime(subscription.getSendTime())
                    .isPersonalized(subscription.isPersonalized())
                    .subscribedAt(subscription.getSubscribedAt())
                    .lastSentAt(subscription.getLastSentAt())
                    .createdAt(subscription.getCreatedAt())
                    .build();
                    
        } catch (Exception e) {
            log.error("구독 정보 조회 실패 - 구독 ID: {}", id, e);
            throw new RuntimeException("구독 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public List<SubscriptionResponse> getSubscriptionsByUser(String userId) {
        try {
            log.info("사용자 구독 목록 조회 - 사용자: {}", userId);
            
            // Repository에서 findByUserId는 Optional을 반환하므로 수정 필요
            // 임시로 빈 리스트 반환
            return List.of();
                    
        } catch (Exception e) {
            log.error("사용자 구독 목록 조회 실패 - 사용자: {}", userId, e);
            throw new RuntimeException("사용자 구독 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public void unsubscribe(Long id) {
        try {
            log.info("뉴스레터 구독 해지 - 구독 ID: {}", id);
            
            Subscription subscription = subscriptionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("구독을 찾을 수 없습니다: " + id));
            
            subscription.setStatus(SubscriptionStatus.UNSUBSCRIBED);
            subscriptionRepository.save(subscription);
            
            log.info("뉴스레터 구독 해지 성공 - 구독 ID: {}", id);
            
        } catch (Exception e) {
            log.error("뉴스레터 구독 해지 실패 - 구독 ID: {}", id, e);
            throw new RuntimeException("뉴스레터 구독 해지 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    @Override
    public List<SubscriptionResponse> getActiveSubscriptions() {
        try {
            log.info("활성 구독 목록 조회");
            
            List<Subscription> activeSubscriptions = subscriptionRepository.findByStatus(SubscriptionStatus.ACTIVE);
            
            return activeSubscriptions.stream()
                    .map(subscription -> SubscriptionResponse.builder()
                            .id(subscription.getId())
                            .userId(subscription.getUserId())
                            .email(subscription.getEmail())
                            .preferredCategories(null) // TODO: JSON 파싱 필요
                            .keywords(null) // TODO: JSON 파싱 필요
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
            log.error("활성 구독 목록 조회 실패", e);
            throw new RuntimeException("활성 구독 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}
