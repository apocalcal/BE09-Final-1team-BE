package com.newsletterservice.repository;

import com.newsletterservice.entity.NewsletterDelivery;
import com.newsletterservice.entity.DeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsletterDeliveryRepository extends JpaRepository<NewsletterDelivery, Long> {

    // 사용자별 발송 기록 조회
    List<NewsletterDelivery> findByUserIdOrderByCreatedAtDesc(Long userId);

    // 뉴스레터별 발송 기록 조회
    List<NewsletterDelivery> findByNewsletterIdOrderByCreatedAtDesc(Long newsletterId);

    // 발송 상태별 조회
    List<NewsletterDelivery> findByStatus(DeliveryStatus status);

    // 특정 기간 발송 기록
    @Query("SELECT nd FROM NewsletterDelivery nd WHERE nd.sentAt BETWEEN :startDate AND :endDate")
    List<NewsletterDelivery> findBySentAtBetween(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    // 발송 실패 건수 조회
    @Query("SELECT COUNT(nd) FROM NewsletterDelivery nd WHERE nd.status = 'FAILED' AND nd.createdAt >= :date")
    Long countFailedDeliveriesAfter(@Param("date") LocalDateTime date);

    // 사용자별 열람률 계산
    @Query("SELECT COUNT(nd) FROM NewsletterDelivery nd WHERE nd.userId = :userId AND nd.openedAt IS NOT NULL")
    Long countOpenedByUserId(@Param("userId") Long userId);

    // 뉴스레터 성과 통계
    @Query("""
        SELECT nd.newsletterId, COUNT(nd) as totalSent, 
               SUM(CASE WHEN nd.openedAt IS NOT NULL THEN 1 ELSE 0 END) as totalOpened
        FROM NewsletterDelivery nd 
        WHERE nd.status = 'SENT' 
        GROUP BY nd.newsletterId
    """)
    List<Object[]> getNewsletterPerformanceStats();
}