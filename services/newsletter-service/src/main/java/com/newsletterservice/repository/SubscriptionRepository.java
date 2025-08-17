package com.newsletterservice.repository;

import com.newsletterservice.entity.Subscription;
import com.newsletterservice.entity.SubscriptionStatus;
import com.newsletterservice.entity.SubscriptionFrequency;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    // 사용자별 구독 정보 조회
    Optional<Subscription> findByUserId(Long userId);

    // 활성 구독자 조회
    List<Subscription> findByStatus(SubscriptionStatus status);

    // 발송 주기별 활성 구독자 조회
    List<Subscription> findByStatusAndFrequency(SubscriptionStatus status, SubscriptionFrequency frequency);

    // 이메일로 구독 정보 조회
    Optional<Subscription> findByEmail(String email);

    // 발송 대상자 조회 (일일 발송)
    @Query("""
        SELECT s FROM Subscription s 
        WHERE s.status = 'ACTIVE' 
        AND s.frequency = 'DAILY'
        AND (s.lastSentAt IS NULL OR s.lastSentAt < :cutoffTime)
        AND (:sendTime IS NULL OR s.sendTime = :sendTime)
    """)
    List<Subscription> findDailyDeliveryTargets(
            @Param("cutoffTime") LocalDateTime cutoffTime,
            @Param("sendTime") Integer sendTime
    );

    // 발송 대상자 조회 (주간 발송)
    @Query("""
        SELECT s FROM Subscription s 
        WHERE s.status = 'ACTIVE' 
        AND s.frequency = 'WEEKLY'
        AND (s.lastSentAt IS NULL OR s.lastSentAt < :cutoffTime)
    """)
    List<Subscription> findWeeklyDeliveryTargets(@Param("cutoffTime") LocalDateTime cutoffTime);

    // 구독자 통계
    @Query("SELECT s.frequency, COUNT(s) FROM Subscription s WHERE s.status = 'ACTIVE' GROUP BY s.frequency")
    List<Object[]> getActiveSubscriptionStats();
}