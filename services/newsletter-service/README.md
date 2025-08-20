# Newsletter Service

뉴스레터 관리 및 발송을 담당하는 마이크로서비스입니다.

## 주요 기능

- 뉴스레터 생성 및 관리
- 구독자 관리
- 이메일 발송
- 뉴스레터 데이터 API 제공
- **개인화된 뉴스레터 콘텐츠 생성**
- **다양한 렌더링 형식 지원 (HTML, JSON)**

## 기술 스택

- Spring Boot 3.5.4
- Spring Cloud 2025.0.0
- Spring Data JPA
- MySQL
- Spring Mail
- Eureka Client

## 실행 방법

1. 환경 변수 설정
```bash
export DB_USERNAME=your_db_username
export DB_PASSWORD=your_db_password
export MAIL_USERNAME=your_email@gmail.com
export MAIL_PASSWORD=your_email_password
```

2. 서비스 실행
```bash
./gradlew bootRun
```

## API 엔드포인트

### 뉴스레터 관리
- `GET /api/v1/newsletters` - 뉴스레터 목록 조회
- `GET /api/v1/newsletters/{id}` - 뉴스레터 상세 조회
- `POST /api/v1/newsletters` - 뉴스레터 생성
- `POST /api/v1/newsletters/{id}/publish` - 뉴스레터 발송
- `DELETE /api/v1/newsletters/{id}` - 뉴스레터 삭제

### 구독 관리
- `POST /api/v1/subscriptions` - 구독 신청
- `DELETE /api/v1/subscriptions/{id}` - 구독 해지

### 뉴스레터 콘텐츠 API (새로운 구조)

#### 개인화된 뉴스레터
- `GET /api/newsletter/{newsletterId}/content` - 개인화된 뉴스레터 콘텐츠 (JSON)
- `GET /api/newsletter/{newsletterId}/html` - 개인화된 뉴스레터 HTML (이메일용)
- `GET /api/newsletter/{newsletterId}/preview` - 뉴스레터 미리보기 (HTML)

#### 카테고리별 뉴스레터
- `POST /api/newsletter/{newsletterId}/category-content` - 카테고리별 뉴스레터 콘텐츠

#### 최신 뉴스 기반 뉴스레터
- `GET /api/newsletter/{newsletterId}/latest-content` - 최신 뉴스 기반 뉴스레터

### 테스트 API
- `GET /api/test/newsletter/{newsletterId}/content` - 개인화 콘텐츠 테스트
- `GET /api/test/newsletter/{newsletterId}/html` - 이메일 HTML 테스트
- `GET /api/test/newsletter/{newsletterId}/category-test` - 카테고리 콘텐츠 테스트

## 데이터베이스

MySQL 데이터베이스 `newsletter_db`를 사용합니다.

## 새로운 아키텍처 구조

### 🎯 **개인화 추천 시스템**
- `PersonalizationService`: 사용자별 개인화된 뉴스 추천
- `UserBehaviorTrackingService`: 사용자 행동 패턴 분석
- 카테고리 선호도, 키워드 매칭, 최신성, 인기도 기반 점수 계산

### 🏗️ **계층 분리 아키텍처**
```
NewsletterContentService (비즈니스 로직)
    ↓
NewsletterContent (구조화된 데이터)
    ↓
EmailNewsletterRenderer (이메일용 HTML)
WebApiRenderer (웹 API용 JSON)
```

### 📊 **데이터 구조**
- `NewsletterContent`: 뉴스레터 콘텐츠의 구조화된 표현
- `Section`: 섹션별 뉴스 그룹핑 (개인화, 트렌딩, 카테고리별)
- `Article`: 개별 뉴스 아티클 정보

### 🔄 **확장성**
- 새로운 렌더링 형식 추가 용이 (PDF, 모바일 앱 등)
- 개인화 로직과 렌더링 로직 완전 분리
- 재사용 가능한 컴포넌트 구조

## 설정

Config Server를 통해 중앙 집중식 설정을 관리합니다.
- Config Server URL: `http://localhost:8888`
- 설정 파일: `newsletter-service.yml`
