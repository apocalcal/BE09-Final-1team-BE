# News Service API Documentation

## 📋 개요
뉴스 서비스의 REST API 문서입니다.

## 🔗 크롤링 관련 API

### 1. 뉴스 저장 API
크롤러에서 수집한 뉴스 데이터를 저장합니다.

```
POST /api/news/crawl
```

**요청 헤더:**
```
Content-Type: application/json
```

**요청 본문:**
```json
{
  "linkId": 123,
  "title": "뉴스 제목",
  "press": "언론사명",
  "content": "뉴스 내용...",
  "reporterName": "기자명",
  "publishedAt": "2024-01-15T10:30:00",
  "categoryId": 1
}
```

**응답:**
- 성공 (200): `"뉴스가 성공적으로 저장되었습니다."`
- 실패 (400): `"저장 실패: [에러 메시지]"`

### 2. 뉴스 미리보기 API
크롤링된 뉴스 데이터를 미리보기합니다 (저장하지 않음).

```
POST /api/news/crawl/preview
```

**요청 헤더:**
```
Content-Type: application/json
```

**요청 본문:** (위와 동일)

**응답:**
- 성공 (200): 미리보기 데이터 반환
```json
{
  "linkId": 123,
  "title": "뉴스 제목",
  "press": "언론사명",
  "content": "뉴스 내용...",
  "reporterName": "기자명",
  "publishedAt": "2024-01-15T10:30:00",
  "categoryId": 1
}
```

### 3. 크롤링된 뉴스 목록 조회 API
저장된 크롤링 뉴스 목록을 조회합니다.

```
GET /api/news/crawl
```

**응답:**
- 성공 (200): 크롤링된 뉴스 목록

## 📊 데이터 모델

### NewsCrawlDto
크롤러에서 전송하는 뉴스 데이터 형식입니다.

| 필드 | 타입 | 설명 | 필수 여부 |
|------|------|------|-----------|
| linkId | Long | 뉴스 링크 ID | ✅ |
| title | String | 뉴스 제목 | ✅ |
| press | String | 언론사명 | ✅ |
| content | String | 뉴스 내용 | ✅ |
| reporterName | String | 기자명 | ❌ |
| publishedAt | LocalDateTime | 발행일시 | ❌ |
| categoryId | Integer | 카테고리 ID | ❌ |

## 🔧 사용 예시

### Python 예시
```python
import requests

# 뉴스 데이터
news_data = {
    "linkId": 123,
    "title": "샘플 뉴스",
    "press": "샘플 언론사",
    "content": "뉴스 내용...",
    "reporterName": "홍길동",
    "publishedAt": "2024-01-15T10:30:00",
    "categoryId": 1
}

# 백엔드로 전송
response = requests.post(
    "http://localhost:8082/api/news/crawl",
    json=news_data,
    headers={"Content-Type": "application/json"}
)

print(response.text)
```

### Java 예시
```java
RestTemplate restTemplate = new RestTemplate();

NewsCrawlDto dto = NewsCrawlDto.builder()
    .linkId(123L)
    .title("샘플 뉴스")
    .press("샘플 언론사")
    .content("뉴스 내용...")
    .reporterName("홍길동")
    .publishedAt(LocalDateTime.now())
    .categoryId(1)
    .build();

String result = restTemplate.postForObject(
    "http://localhost:8082/api/news/crawl",
    dto,
    String.class
);
```

## ⚠️ 주의사항

1. **중복 방지**: 같은 `linkId`로 중복 저장 시 에러 발생
2. **데이터 검증**: 필수 필드 누락 시 저장 실패
3. **CORS 설정**: `@CrossOrigin(origins = "*")` 설정으로 크로스 오리진 요청 허용
4. **에러 처리**: 네트워크 오류, DB 오류 등 예외 상황 처리

## 📈 성능 고려사항

1. **대량 데이터**: 많은 뉴스를 한 번에 전송할 때는 배치 처리 고려
2. **네트워크 타임아웃**: 긴 뉴스 내용 전송 시 타임아웃 설정
3. **메모리 사용량**: 큰 뉴스 내용 전송 시 메모리 사용량 모니터링 