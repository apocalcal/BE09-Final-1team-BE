# News Service API 엔드포인트

뉴스 서비스의 모든 API 엔드포인트 문서입니다.

## 기본 정보

- **Base URL**: `http://localhost:8083/news-service/api/news`
- **Gateway를 통한 접근**: `http://localhost:8000/news-service/api/news`

---

## 1. 헬스 체크

### GET /health

서비스 상태를 확인합니다.

**Response:**

```
News Service is running
```

---

## 2. 뉴스 목록 조회

### GET /

모든 뉴스 목록을 페이징으로 조회합니다.

**Parameters:**

- `page` (int, optional): 페이지 번호 (기본값: 0)
- `size` (int, optional): 페이지 크기 (기본값: 20)

**Example:**

```
GET /api/news?page=0&size=10
```

**Response:**

```json
{
  "content": [
    {
      "newsId": 1,
      "categoryName": "POLITICS",
      "title": "뉴스 제목",
      "press": "연합뉴스",
      "publishedAt": "2025-08-07T10:30:00",
      "reporter": "홍길동",
      "createdAt": "2025-08-07T10:30:00"
    }
  ],
  "pageable": {...},
  "totalElements": 100,
  "totalPages": 10,
  "first": true,
  "last": false
}
```

---

## 3. 뉴스 상세 조회

### GET /{newsId}

특정 뉴스의 상세 정보를 조회합니다.

**Parameters:**

- `newsId` (Long): 뉴스 ID

**Example:**

```
GET /api/news/1
```

**Response:**

```json
{
  "newsId": 1,
  "categoryName": "POLITICS",
  "title": "뉴스 제목",
  "content": "뉴스 본문 내용...",
  "press": "연합뉴스",
  "publishedAt": "2025-08-07T10:30:00",
  "reporter": "홍길동",
  "dedupState": "REPRESENTATIVE",
  "createdAt": "2025-08-07T10:30:00",
  "updatedAt": "2025-08-07T10:30:00"
}
```

---

## 4. 카테고리별 뉴스 조회

### GET /category/{categoryName}

특정 카테고리의 뉴스 목록을 조회합니다.

**Parameters:**

- `categoryName` (String): 카테고리명 (POLITICS, ECONOMY, SOCIETY, CULTURE, INTERNATIONAL, IT)
- `page` (int, optional): 페이지 번호 (기본값: 0)
- `size` (int, optional): 페이지 크기 (기본값: 20)

**Example:**

```
GET /api/news/category/POLITICS?page=0&size=10
GET /api/news/category/IT
```

**Response:** 페이징된 뉴스 목록 (위 `/` 엔드포인트와 동일한 형식)

---

## 5. 뉴스 검색

### GET /search

제목에 키워드가 포함된 뉴스를 검색합니다.

**Parameters:**

- `keyword` (String, required): 검색 키워드
- `page` (int, optional): 페이지 번호 (기본값: 0)
- `size` (int, optional): 페이지 크기 (기본값: 20)

**Example:**

```
GET /api/news/search?keyword=경제&page=0&size=10
```

**Response:** 페이징된 뉴스 목록

---

## 6. 언론사별 뉴스 조회

### GET /press/{press}

특정 언론사의 뉴스 목록을 조회합니다.

**Parameters:**

- `press` (String): 언론사명
- `page` (int, optional): 페이지 번호 (기본값: 0)
- `size` (int, optional): 페이지 크기 (기본값: 20)

**Example:**

```
GET /api/news/press/연합뉴스?page=0&size=10
```

**Response:** 페이징된 뉴스 목록

---

## 7. 최신 뉴스 조회

### GET /latest

최신 뉴스 목록을 조회합니다.

**Parameters:**

- `limit` (int, optional): 조회할 뉴스 개수 (기본값: 10)

**Example:**

```
GET /api/news/latest?limit=5
```

**Response:**

```json
[
  {
    "newsId": 1,
    "categoryName": "POLITICS",
    "title": "뉴스 제목",
    "press": "연합뉴스",
    "publishedAt": "2025-08-07T10:30:00",
    "reporter": "홍길동",
    "createdAt": "2025-08-07T10:30:00"
  }
]
```

---

## 8. 기간별 뉴스 조회

### GET /date-range

특정 기간의 뉴스를 조회합니다.

**Parameters:**

- `startDate` (String, required): 시작일시 (형식: yyyy-MM-dd'T'HH:mm:ss)
- `endDate` (String, required): 종료일시 (형식: yyyy-MM-dd'T'HH:mm:ss)

**Example:**

```
GET /api/news/date-range?startDate=2025-08-01T00:00:00&endDate=2025-08-07T23:59:59
```

**Response:** 뉴스 목록 배열

---

## 9. 뉴스 개수 조회

### GET /count

전체 뉴스 개수를 조회합니다.

**Example:**

```
GET /api/news/count
```

**Response:**

```json
1250
```

---

## 10. 카테고리별 뉴스 개수 조회

### GET /count/category/{categoryName}

특정 카테고리의 뉴스 개수를 조회합니다.

**Parameters:**

- `categoryName` (String): 카테고리명

**Example:**

```
GET /api/news/count/category/POLITICS
```

**Response:**

```json
350
```

---

## 11. 중복제거 상태별 뉴스 조회

### GET /dedup/{dedupState}

중복제거 상태별로 뉴스를 조회합니다.

**Parameters:**

- `dedupState` (String): 중복제거 상태 (REPRESENTATIVE, RELATED, KEPT)

**Example:**

```
GET /api/news/dedup/REPRESENTATIVE
```

**Response:** 뉴스 목록 배열

---

## 12. 지원 카테고리 목록 조회

### GET /categories

서비스에서 지원하는 카테고리 목록을 조회합니다.

**Example:**

```
GET /api/news/categories
```

**Response:**

```json
[
  {
    "code": "POLITICS",
    "name": "정치"
  },
  {
    "code": "ECONOMY",
    "name": "경제"
  },
  {
    "code": "SOCIETY",
    "name": "사회"
  },
  {
    "code": "CULTURE",
    "name": "문화"
  },
  {
    "code": "INTERNATIONAL",
    "name": "국제"
  },
  {
    "code": "IT",
    "name": "IT"
  }
]
```

---

## 카테고리 코드

| 코드          | 한국어명 |
| ------------- | -------- |
| POLITICS      | 정치     |
| ECONOMY       | 경제     |
| SOCIETY       | 사회     |
| CULTURE       | 문화     |
| INTERNATIONAL | 국제     |
| IT            | IT       |

## 중복제거 상태 코드

| 코드           | 한국어명 |
| -------------- | -------- |
| REPRESENTATIVE | 대표     |
| RELATED        | 관련     |
| KEPT           | 보관     |

---

## 에러 응답

### 400 Bad Request

- 잘못된 카테고리명이나 중복제거 상태를 요청한 경우

### 404 Not Found

- 존재하지 않는 뉴스 ID를 요청한 경우

### 500 Internal Server Error

- 서버 내부 오류

**에러 응답 형식:**

```json
{
  "path": "/api/news/1",
  "error": "Internal Server Error",
  "message": "서버 내부 오류가 발생했습니다.",
  "timestamp": "2025-08-07T10:47:59.612+09:00"
}
```

---

## Postman 테스트 예시

### 1. 헬스 체크

```
GET http://localhost:8083/news-service/api/news/health
```

### 2. 전체 뉴스 조회 (첫 번째 페이지)

```
GET http://localhost:8083/news-service/api/news?page=0&size=5
```

### 3. IT 카테고리 뉴스 조회

```
GET http://localhost:8083/news-service/api/news/category/IT
```

### 4. 경제 관련 뉴스 검색

```
GET http://localhost:8083/news-service/api/news/search?keyword=경제
```

### 5. 최신 뉴스 3개 조회

```
GET http://localhost:8083/news-service/api/news/latest?limit=3
```
