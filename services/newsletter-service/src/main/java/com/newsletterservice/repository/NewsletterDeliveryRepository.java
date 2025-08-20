package com.newsletterservice.repository;

import com.newsletterservice.entity.NewsletterDelivery;
import com.newsletterservice.entity.DeliveryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsletterDeliveryRepository extends JpaRepository<NewsletterDelivery, Long> {

    // 사용자별 발송 기록 조회(최신순)
    Page<NewsletterDelivery> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 뉴스레터별 발송 기록 조회
    List<NewsletterDelivery> findByNewsletterIdOrderByCreatedAtDesc(Long newsletterId);

    // 발송 상태별 조회
    Page<NewsletterDelivery> findByStatus(DeliveryStatus status, Pageable pageable);

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

    List<NewsletterDelivery> findByStatusAndUpdatedAtAfterAndRetryCountLessThan(DeliveryStatus deliveryStatus, LocalDateTime cutoff, int i);

    long countByStatusAndCreatedAtBetween(DeliveryStatus deliveryStatus, LocalDateTime startDate, LocalDateTime endDate);

    // 예약된 발송 목록 조회 (PENDING 상태이고 scheduledAt이 현재 시간보다 이전인 것들)
    @Query("SELECT nd FROM NewsletterDelivery nd WHERE nd.status = :status AND nd.scheduledAt <= :now")
    List<NewsletterDelivery> findByStatusAndScheduledAtBefore(
            @Param("status") DeliveryStatus status, 
            @Param("now") LocalDateTime now);

    // 오래된 발송 기록 조회 (정리용)
    List<NewsletterDelivery> findByCreatedAtBeforeAndStatusIn(LocalDateTime cutoff, List<DeliveryStatus> statuses);
}