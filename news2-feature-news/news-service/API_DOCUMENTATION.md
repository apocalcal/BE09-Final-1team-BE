# 뉴스 서비스 API 문서

## 📋 개요
뉴스 서비스는 트렌딩, 추천, 카테고리별 필터링, 검색 기능을 제공하는 RESTful API입니다.

## 🔗 기본 URL
```
http://localhost:8080/api/news
```

## 📊 API 엔드포인트

### 🔥 트렌딩 뉴스
**GET** `/api/news/trending`

신뢰도와 조회수를 기반으로 한 인기 뉴스를 조회합니다.

**Query Parameters:**
- `page` (optional): 페이지 번호 (기본값: 0)
- `size` (optional): 페이지 크기 (기본값: 20)

**Response:**
```json
{
  "content": [
    {
      "newsId": 1,
      "title": "AI 기술 발전으로 일자리 변화 예상",
      "summary": "인공지능 기술의 발전으로 많은 직업이 변화할 것으로 예상됩니다...",
      "press": "조선일보",
      "category": "IT/과학",
      "publishedAt": "2025-01-03T10:00:00",
      "trusted": 90,
      "views": 150
    }
  ],
  "totalElements": 10,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

### 🎯 추천 뉴스
**GET** `/api/news/recommendations`

사용자의 관심 카테고리 기반으로 추천 뉴스를 조회합니다.

**Query Parameters:**
- `userId` (optional): 사용자 ID (기본값: 1)
- `page` (optional): 페이지 번호
- `size` (optional): 페이지 크기

**Response:** 트렌딩 뉴스와 동일한 형식

### 🗂️ 카테고리별 뉴스
**GET** `/api/news/category/{categoryId}`

특정 카테고리의 뉴스를 조회합니다.

**Path Parameters:**
- `categoryId`: 카테고리 ID (1: 경제, 2: 정치, 3: 사회, 4: 문화, 5: IT/과학, 6: 스포츠, 7: 국제)

**Query Parameters:**
- `page` (optional): 페이지 번호
- `size` (optional): 페이지 크기

**Response:** 트렌딩 뉴스와 동일한 형식

### 🔍 키워드 검색
**GET** `/api/news/search`

제목, 요약, 내용에서 키워드를 검색합니다.

**Query Parameters:**
- `query` (required): 검색 키워드
- `page` (optional): 페이지 번호
- `size` (optional): 페이지 크기

**Response:** 트렌딩 뉴스와 동일한 형식

### 📊 인기 뉴스 (조회수 기반)
**GET** `/api/news/popular`

조회수를 기준으로 한 인기 뉴스를 조회합니다.

**Query Parameters:**
- `page` (optional): 페이지 번호
- `size` (optional): 페이지 크기

**Response:** 트렌딩 뉴스와 동일한 형식

### 📰 최신 뉴스
**GET** `/api/news/latest`

최신 등록 순으로 뉴스를 조회합니다.

**Query Parameters:**
- `page` (optional): 페이지 번호
- `size` (optional): 페이지 크기

**Response:** 트렌딩 뉴스와 동일한 형식

### 📰 뉴스 상세 조회
**GET** `/api/news/{newsId}`

특정 뉴스의 상세 정보를 조회합니다.

**Path Parameters:**
- `newsId`: 뉴스 ID

**Response:**
```json
{
  "newsId": 1,
  "originalNewsId": 1,
  "title": "AI 기술 발전으로 일자리 변화 예상",
  "content": "인공지능 기술의 발전으로 많은 직업이 변화할 것으로 예상됩니다...",
  "press": "조선일보",
  "link": "https://example.com/news1",
  "summary": "인공지능 기술의 발전으로 많은 직업이 변화할 것으로 예상됩니다...",
  "trusted": 90,
  "publishedAt": "2025-01-03T10:00:00",
  "createdAt": "2025-01-03T10:00:00",
  "reporterName": "테스트 기자 1"
}
```

### 👁️ 조회수 증가
**POST** `/api/news/{newsId}/view`

뉴스 조회수를 증가시킵니다.

**Path Parameters:**
- `newsId`: 뉴스 ID

**Response:** 200 OK

## 🧪 테스트

### 테스트 데이터 생성
**POST** `/api/news/test-data`

다양한 카테고리의 테스트 데이터를 생성합니다.

**Response:**
```json
"5개의 테스트 데이터가 생성되었습니다."
```

## 📝 사용 예시

### 1. 트렌딩 뉴스 조회
```bash
curl -X GET "http://localhost:8080/api/news/trending?page=0&size=10"
```

### 2. 카테고리별 뉴스 조회
```bash
curl -X GET "http://localhost:8080/api/news/category/1?page=0&size=10"
```

### 3. 키워드 검색
```bash
curl -X GET "http://localhost:8080/api/news/search?query=AI&page=0&size=10"
```

### 4. 뉴스 조회수 증가
```bash
curl -X POST "http://localhost:8080/api/news/1/view"
```

## 🔧 기술 스택

- **Framework**: Spring Boot 3.x
- **Database**: JPA/Hibernate
- **Build Tool**: Gradle
- **Language**: Java 17

## 📊 데이터베이스 스키마

### News 테이블
- `news_id`: 뉴스 ID (PK)
- `original_news_id`: 원본 뉴스 ID (FK)
- `published_at`: 발행일
- `summary`: 요약
- `trusted`: 신뢰도 점수
- `views`: 조회수
- `created_at`: 생성일
- `updated_at`: 수정일

### NewsCrawl 테이블
- `raw_id`: 원본 ID (PK)
- `link_id`: 링크 ID
- `link`: 뉴스 링크
- `title`: 제목
- `press`: 언론사
- `content`: 내용
- `reporter_name`: 기자명
- `category_id2`: 카테고리 ID
- `created_at`: 생성일

## 🚀 향후 개선 사항

1. **개인화 추천**: 사용자 관심사 기반 정교한 추천 알고리즘
2. **실시간 트렌딩**: 실시간 조회수 기반 트렌딩 계산
3. **AI 요약**: GPT 등 AI 모델을 활용한 자동 요약
4. **감정 분석**: 뉴스 내용의 감정 분석 및 분류
5. **댓글 시스템**: 뉴스별 댓글 기능
6. **북마크**: 사용자별 뉴스 북마크 기능 