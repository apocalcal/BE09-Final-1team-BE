#!/bin/bash

# 키워드 구독 API 테스트 스크립트
BASE_URL="http://localhost:8080"

echo "=== 키워드 구독 API 테스트 ==="

# 1. 키워드 구독
echo "1. 키워드 구독 테스트"
curl -X POST "$BASE_URL/api/keywords/subscribe?userId=1&keyword=AI" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n\n"

# 2. 사용자의 키워드 구독 목록 조회
echo "2. 사용자 키워드 구독 목록 조회"
curl -X GET "$BASE_URL/api/keywords/user/1" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n\n"

# 3. 키워드 구독 해제
echo "3. 키워드 구독 해제"
curl -X DELETE "$BASE_URL/api/keywords/unsubscribe?userId=1&keyword=AI" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n\n"

echo "=== 트렌딩 키워드 API 테스트 ==="

# 4. 트렌딩 키워드 조회
echo "4. 트렌딩 키워드 조회"
curl -X GET "$BASE_URL/api/trending/keywords?limit=5" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n\n"

# 5. 인기 키워드 조회
echo "5. 인기 키워드 조회"
curl -X GET "$BASE_URL/api/trending/keywords/popular?limit=5" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n\n"

echo "=== 검색 API 필터링 테스트 ==="

# 6. 검색 API에 필터링 적용
echo "6. 검색 API 필터링 테스트"
curl -X GET "$BASE_URL/api/search?query=AI&sortBy=date&sortOrder=desc&category=IT_SCIENCE&limit=5" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n\n"

echo "테스트 완료!"
