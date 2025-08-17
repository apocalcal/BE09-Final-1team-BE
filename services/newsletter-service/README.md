# Newsletter Service

뉴스레터 관리 및 발송을 담당하는 마이크로서비스입니다.

## 주요 기능

- 뉴스레터 생성 및 관리
- 구독자 관리
- 이메일 발송
- 뉴스레터 템플릿 관리

## 기술 스택

- Spring Boot 3.5.4
- Spring Cloud 2025.0.0
- Spring Data JPA
- MySQL
- Spring Mail
- Thymeleaf
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

## 데이터베이스

MySQL 데이터베이스 `newsletter_db`를 사용합니다.

## 설정

Config Server를 통해 중앙 집중식 설정을 관리합니다.
- Config Server URL: `http://localhost:8888`
- 설정 파일: `newsletter-service.yml`
