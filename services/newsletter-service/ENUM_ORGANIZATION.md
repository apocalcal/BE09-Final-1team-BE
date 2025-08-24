# Newsletter Service Enum Organization

This document summarizes the enum files that have been organized and created for the newsletter service.

## Created/Updated Enum Files

### 1. Content & Personalization
- **ContentLength.java** - 콘텐츠 길이 (SHORT, MEDIUM, LONG, EXTENDED)
- **ContentPriority.java** - 콘텐츠 우선순위 (URGENT, HIGH, MEDIUM, LOW, OPTIONAL)
- **PersonalizationLevel.java** - 개인화 수준 (NONE, BASIC, INTERMEDIATE, ADVANCED, PREMIUM)
- **RecommendationType.java** - 추천 알고리즘 타입 (CONTENT_BASED, COLLABORATIVE, HYBRID, etc.)

### 2. Delivery & Subscription
- **DeliveryMethod.java** - 배송 방법 (EMAIL, SMS, PUSH) - Updated with level field
- **DeliveryStatus.java** - 배송 상태 (PENDING, PROCESSING, SENT, etc.) - Already existed
- **SubscriptionFrequency.java** - 구독 빈도 (DAILY, WEEKLY, MONTHLY, IMMEDIATE) - Updated
- **SubscriptionStatus.java** - 구독 상태 (ACTIVE, INACTIVE, etc.) - Already existed

### 3. Newsletter & Templates
- **NewsletterType.java** - 뉴스레터 타입 (POLITICS_DAILY, ECONOMY_DAILY, etc.)
- **EmailTemplateType.java** - 이메일 템플릿 타입 (SIMPLE, NEWSLETTER, PROMOTIONAL, etc.)
- **TemplateStyle.java** - 템플릿 스타일 (MINIMALIST, CORPORATE, MODERN, etc.)

### 4. Notification & Communication
- **NotificationType.java** - 알림 타입 (SUBSCRIPTION_CONFIRMED, DELIVERY_SENT, etc.)
- **NotificationPriority.java** - 알림 우선순위 (LOW, NORMAL, HIGH, URGENT)

### 5. Analysis & Performance
- **PerformanceMetric.java** - 성과 분석 지표 (OPEN_RATE, CLICK_RATE, etc.)
- **AnalysisPeriod.java** - 분석 기간 (LAST_7_DAYS, LAST_30_DAYS, etc.)
- **EngagementLevel.java** - 참여도 수준 (VERY_LOW, LOW, MEDIUM, HIGH, VERY_HIGH)
- **QualityGrade.java** - 품질 점수 등급 (S, A, B, C, D)

### 6. User & Interaction
- **UserSegment.java** - 사용자 세그먼트 (NEW_USER, ACTIVE_USER, etc.)
- **InteractionType.java** - 사용자 상호작용 타입 (VIEW, CLICK, SHARE, etc.) - Already existed
- **NewsCategory.java** - 뉴스 카테고리 (POLITICS, ECONOMY, etc.) - Updated

### 7. System & Technical
- **ServiceStatus.java** - 서비스 상태 (HEALTHY, DEGRADED, UNHEALTHY, etc.)
- **LogLevel.java** - 로그 레벨 (TRACE, DEBUG, INFO, WARN, ERROR, FATAL)
- **ABTestStatus.java** - A/B 테스트 상태 (DRAFT, RUNNING, PAUSED, etc.)
- **ABTestGoal.java** - A/B 테스트 목표 (OPEN_RATE, CLICK_RATE, etc.)

### 8. Sorting & Filtering
- **SortBy.java** - 정렬 기준 (CREATED_AT, UPDATED_AT, TITLE, etc.)
- **SortOrder.java** - 정렬 방향 (ASC, DESC)

### 9. Curation & Strategy
- **CurationStrategy.java** - 큐레이션 전략 (LATEST, POPULAR, TRENDING, etc.)

## Key Features

### Consistent Structure
All enums follow the same pattern:
- Use Lombok `@Getter` and `@AllArgsConstructor` annotations
- Use `description` field instead of `displayName` for consistency
- Proper package declaration: `package com.newsletterservice.entity;`

### Utility Methods
Some enums include utility methods:
- **EngagementLevel.fromScore(double score)** - Convert score to engagement level
- **QualityGrade.fromScore(double score)** - Convert score to quality grade

### Integration
- **NewsletterType** references **NewsCategory** for default categories
- All enums are properly organized in the entity package
- Consistent naming conventions throughout

## Usage Examples

```java
// Get description
String description = DeliveryMethod.EMAIL.getDescription(); // "이메일"

// Get level
int level = DeliveryMethod.EMAIL.getLevel(); // 1

// Convert score to level
EngagementLevel level = EngagementLevel.fromScore(75.5); // HIGH

// Get quality grade
QualityGrade grade = QualityGrade.fromScore(85.0); // A
```

## Notes

- All enums are now properly organized in separate files
- Consistent structure with Lombok annotations
- Proper field naming (`description` instead of `displayName`)
- Utility methods where appropriate
- Integration with existing entity structure
