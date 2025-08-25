# 📧 NewsletterDeliveryRepository 상세 가이드

## 🎯 개요

`NewsletterDeliveryRepository`는 뉴스레터 발송 기록을 관리하는 핵심 데이터 액세스 계층입니다. 발송 상태 추적, 성과 분석, 사용자별 발송 이력 관리의 모든 기능을 제공합니다.

## 🏗️ 아키텍처 개선사항

### 1. **인덱스 최적화**
```sql
-- 자주 사용되는 조회 패턴에 맞춘 복합 인덱스
CREATE INDEX idx_user_id_created_at ON newsletter_delivery (user_id, created_at DESC);
CREATE INDEX idx_newsletter_id_created_at ON newsletter_delivery (newsletter_id, created_at DESC);
CREATE INDEX idx_status_created_at ON newsletter_delivery (status, created_at);
CREATE INDEX idx_delivery_method_created_at ON newsletter_delivery (delivery_method, created_at);

-- 시간 기반 조회 최적화
CREATE INDEX idx_sent_at ON newsletter_delivery (sent_at);
CREATE INDEX idx_opened_at ON newsletter_delivery (opened_at);
CREATE INDEX idx_scheduled_at ON newsletter_delivery (scheduled_at);

-- 복합 인덱스
CREATE INDEX idx_user_status ON newsletter_delivery (user_id, status);
CREATE INDEX idx_newsletter_status ON newsletter_delivery (newsletter_id, status);
CREATE INDEX idx_status_method ON newsletter_delivery (status, delivery_method);
```

### 2. **성능 최적화된 쿼리**
- **배치 처리**: 대량 데이터 처리 시 성능 향상
- **커버링 인덱스**: 테이블 액세스 최소화
- **집계 쿼리**: 통계 데이터 조회 최적화

## 📊 주요 기능

### 1. **기본 조회 메서드**
```java
// 사용자별 발송 기록 조회
Page<NewsletterDelivery> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

// 뉴스레터별 발송 기록 조회
List<NewsletterDelivery> findByNewsletterIdOrderByCreatedAtDesc(Long newsletterId);

// 발송 상태별 조회
Page<NewsletterDelivery> findByStatus(DeliveryStatus status, Pageable pageable);

// 발송 방법별 조회
List<NewsletterDelivery> findByDeliveryMethod(DeliveryMethod method);
```

### 2. **복합 조건 조회**
```java
// 사용자별 + 상태별 조회
List<NewsletterDelivery> findByUserIdAndStatus(Long userId, DeliveryStatus status);

// 뉴스레터별 + 상태별 조회
List<NewsletterDelivery> findByNewsletterIdAndStatus(Long newsletterId, DeliveryStatus status);

// 기간별 + 상태별 조회
List<NewsletterDelivery> findByStatusAndCreatedAtBetween(
    DeliveryStatus status, LocalDateTime startDate, LocalDateTime endDate);
```

### 3. **시간 기반 조회**
```java
// 특정 기간 발송 기록 조회
@Query("SELECT nd FROM NewsletterDelivery nd WHERE nd.sentAt BETWEEN :startDate AND :endDate")
List<NewsletterDelivery> findBySentAtBetween(LocalDateTime startDate, LocalDateTime endDate);

// 최근 N일간 사용자별 발송 기록
@Query("SELECT nd FROM NewsletterDelivery nd WHERE nd.userId = :userId AND nd.createdAt >= :sinceDate")
List<NewsletterDelivery> findByUserIdAndCreatedAtAfter(Long userId, LocalDateTime sinceDate);

// 예약 발송 대상 조회
@Query("SELECT nd FROM NewsletterDelivery nd WHERE nd.status = 'PENDING' AND nd.scheduledAt <= :now")
List<NewsletterDelivery> findPendingDeliveriesForSchedule(LocalDateTime now);
```

### 4. **통계 및 집계**
```java
// 발송 실패 건수 조회
@Query("SELECT COUNT(nd) FROM NewsletterDelivery nd WHERE nd.status = 'FAILED' AND nd.createdAt >= :date")
Long countFailedDeliveriesAfter(LocalDateTime date);

// 사용자별 열람 건수
@Query("SELECT COUNT(nd) FROM NewsletterDelivery nd WHERE nd.userId = :userId AND nd.openedAt IS NOT NULL")
Long countOpenedByUserId(Long userId);

// 뉴스레터별 성과 통계
@Query("""
    SELECT nd.newsletterId, COUNT(nd) as totalSent, 
           SUM(CASE WHEN nd.openedAt IS NOT NULL THEN 1 ELSE 0 END) as totalOpened,
           SUM(CASE WHEN nd.status = 'FAILED' THEN 1 ELSE 0 END) as totalFailed,
           AVG(CASE WHEN nd.sentAt IS NOT NULL AND nd.createdAt IS NOT NULL 
               THEN TIMESTAMPDIFF(SECOND, nd.createdAt, nd.sentAt) 
               ELSE NULL END) as avgDeliveryTimeSeconds
    FROM NewsletterDelivery nd 
    WHERE nd.createdAt >= :since
    GROUP BY nd.newsletterId
    ORDER BY totalSent DESC
""")
List<Object[]> getNewsletterPerformanceStats(LocalDateTime since);
```

### 5. **업데이트 및 삭제**
```java
// 발송 상태 일괄 업데이트
@Modifying
@Query("UPDATE NewsletterDelivery nd SET nd.status = :newStatus WHERE nd.status = :currentStatus")
int bulkUpdateStatus(DeliveryStatus currentStatus, DeliveryStatus newStatus);

// 열람 시간 업데이트
@Modifying
@Query("UPDATE NewsletterDelivery nd SET nd.status = 'OPENED', nd.openedAt = :openedAt WHERE nd.id = :deliveryId")
int markAsOpened(Long deliveryId, LocalDateTime openedAt);

// 오래된 발송 기록 정리
@Modifying
@Query("DELETE FROM NewsletterDelivery nd WHERE nd.createdAt < :cutoffDate AND nd.status IN ('FAILED', 'BOUNCED')")
int deleteOldFailedDeliveries(LocalDateTime cutoffDate);
```

## 🚀 실제 사용 시나리오

### 1. **사용자 마이페이지 구현**
```java
@RestController
public class UserNewsletterController {
    
    @GetMapping("/api/user/newsletters")
    public ApiResponse<Page<NewsletterDeliveryResponse>> getUserNewsletters(
        @RequestParam Long userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<NewsletterDelivery> deliveries = deliveryRepository
            .findByUserIdOrderByCreatedAtDesc(userId, pageable);
        
        // DTO 변환 및 반환
        return ApiResponse.success(deliveries.map(this::toResponse));
    }
}
```

### 2. **관리자 대시보드**
```java
@RestController
public class AdminDashboardController {
    
    @GetMapping("/api/admin/delivery/realtime-stats")
    public ApiResponse<RealTimeStatsDto> getRealTimeStats() {
        
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        Object[] stats = deliveryRepository.getRealTimeStats(since);
        
        RealTimeStatsDto dto = RealTimeStatsDto.builder()
            .pendingCount(((Number) stats[0]).longValue())
            .processingCount(((Number) stats[1]).longValue())
            .sentCount(((Number) stats[2]).longValue())
            .openedCount(((Number) stats[3]).longValue())
            .failedCount(((Number) stats[4]).longValue())
            .bouncedCount(((Number) stats[5]).longValue())
            .build();
        
        return ApiResponse.success(dto);
    }
}
```

### 3. **발송 스케줄러**
```java
@Component
public class NewsletterDeliveryScheduler {
    
    @Scheduled(fixedRate = 60000) // 1분마다
    public void processPendingDeliveries() {
        
        LocalDateTime now = LocalDateTime.now();
        List<NewsletterDelivery> pendingDeliveries = deliveryRepository
            .findPendingDeliveriesForSchedule(now);
        
        for (NewsletterDelivery delivery : pendingDeliveries) {
            processDelivery(delivery);
        }
    }
}
```

### 4. **이메일 추적**
```java
@RestController
public class EmailTrackingController {
    
    @GetMapping("/api/track/open/{deliveryId}")
    public ResponseEntity<byte[]> trackEmailOpen(@PathVariable Long deliveryId) {
        
        int updatedRows = deliveryRepository.markAsOpened(deliveryId, LocalDateTime.now());
        
        if (updatedRows > 0) {
            log.info("Email opened: deliveryId={}", deliveryId);
        }
        
        // 1x1 투명 픽셀 이미지 반환
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(generateTrackingPixel());
    }
}
```

## 📈 성능 최적화 전략

### 1. **데이터베이스 연결 풀 최적화**
```java
@Configuration
public class DatabaseOptimizationConfig {
    
    @Bean
    public DataSource primaryDataSource() {
        HikariConfig config = new HikariConfig();
        
        // 연결 풀 최적화
        config.setMaximumPoolSize(20);
        config.setMinimumIdle(5);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        
        // 성능 최적화 설정
        config.setLeakDetectionThreshold(60000);
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        
        return new HikariDataSource(config);
    }
}
```

### 2. **JPA 배치 처리 최적화**
```properties
# application.yml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 50
        order_inserts: true
        order_updates: true
        jdbc:
          batch_versioned_data: true
```

### 3. **캐싱 전략**
```java
@Service
@CacheConfig(cacheNames = "newsletter-delivery")
public class CachedDeliveryService {
    
    @Cacheable(key = "'user-stats:' + #userId + ':' + #days")
    public UserEngagementStats getUserEngagementStats(Long userId, int days) {
        // 캐시된 데이터 반환
    }
    
    @CacheEvict(allEntries = true)
    public void evictAllCaches() {
        // 캐시 무효화
    }
}
```

## 🔍 모니터링 및 알림

### 1. **성능 지표 수집**
```java
@Component
public class DeliveryPerformanceMonitor {
    
    @Scheduled(fixedRate = 300000) // 5분마다
    public void collectPerformanceMetrics() {
        
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        Long failedCount = deliveryRepository.countFailedDeliveriesAfter(oneHourAgo);
        
        // 임계값 초과 시 알림
        if (failedCount > 50) {
            alertService.sendCriticalAlert(
                String.format("높은 발송 실패율 감지: %d건 (최근 1시간)", failedCount)
            );
        }
    }
}
```

### 2. **실시간 대시보드**
```java
@RestController
public class RealTimeDashboardController {
    
    @GetMapping("/api/admin/realtime-stats")
    public ApiResponse<RealTimeStatsDto> getRealTimeStats() {
        
        LocalDateTime since = LocalDateTime.now().minusHours(1);
        Object[] stats = deliveryRepository.getRealTimeStats(since);
        
        // 이상 상태 감지
        if (((Number) stats[4]).longValue() > 100) {
            alertService.sendAlert("발송 실패 건수가 비정상적으로 높습니다");
        }
        
        return ApiResponse.success(buildRealTimeStatsDto(stats));
    }
}
```

## 🛠️ 확장성 고려사항

### 1. **데이터 증가에 따른 대응**
- **월 1천만 건**: 인덱스 최적화
- **월 1억 건**: 파티셔닝 도입
- **월 10억 건**: 샤딩 + 아카이빙

### 2. **파티셔닝 전략**
```sql
-- 날짜 기반 파티셔닝
CREATE TABLE newsletter_delivery_2024_01 PARTITION OF newsletter_delivery
FOR VALUES FROM ('2024-01-01') TO ('2024-02-01');

CREATE TABLE newsletter_delivery_2024_02 PARTITION OF newsletter_delivery
FOR VALUES FROM ('2024-02-01') TO ('2024-03-01');
```

### 3. **아카이빙 전략**
```java
@Service
public class DeliveryArchiveService {
    
    @Scheduled(cron = "0 0 1 1 * *") // 매월 1일 새벽 1시
    public void archiveOldDeliveries() {
        
        LocalDateTime archiveCutoff = LocalDateTime.now().minusMonths(6);
        List<NewsletterDelivery> oldDeliveries = deliveryRepository
            .findByCreatedAtBefore(archiveCutoff);
        
        // 아카이브 테이블로 이동
        archiveTableService.insertBatch(oldDeliveries);
    }
}
```

## 📋 체크리스트

### 개발 단계
- [ ] 인덱스 설계 및 적용
- [ ] 쿼리 성능 테스트
- [ ] 배치 처리 최적화
- [ ] 캐싱 전략 구현
- [ ] 모니터링 시스템 구축

### 운영 단계
- [ ] 성능 지표 모니터링
- [ ] 알림 시스템 설정
- [ ] 백업 및 복구 전략
- [ ] 확장성 계획 수립
- [ ] 정기적인 성능 튜닝

## 🎯 핵심 인사이트

1. **인덱스 전략의 중요성**: 자주 사용되는 쿼리 패턴에 맞춘 복합 인덱스 설계
2. **배치 처리 최적화**: 대량 데이터 처리 시 성능 향상
3. **실시간 모니터링**: 시스템 안정성과 성능 지속적 추적
4. **확장성 고려**: 데이터 증가에 따른 아키텍처 진화 계획

**NewsletterDeliveryRepository는 단순한 데이터 액세스 계층이 아니라, 전체 뉴스레터 시스템의 성능과 안정성을 결정하는 핵심 컴포넌트입니다!**
