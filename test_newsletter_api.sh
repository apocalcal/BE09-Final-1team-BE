#!/bin/bash

# 뉴스레터 API 테스트 스크립트
BASE_URL="http://localhost:8085"

echo "=== 뉴스레터 API 테스트 ==="

# 1. 헬스체크
echo "1. 헬스체크..."
curl -X GET "${BASE_URL}/actuator/health" \
  -w "\nHTTP Status: %{http_code}\n\n"

# 2. 카테고리별 헤드라인 조회
echo "2. 카테고리별 헤드라인 조회..."
curl -X GET "${BASE_URL}/api/newsletter/category/정치/headlines?limit=5" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n\n"

# 3. 카테고리별 기사 조회
echo "3. 카테고리별 기사 조회..."
curl -X GET "${BASE_URL}/api/newsletter/category/정치/articles?limit=5" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n\n"

# 4. 카테고리별 트렌드 키워드 조회
echo "4. 카테고리별 트렌드 키워드 조회..."
curl -X GET "${BASE_URL}/api/newsletter/category/정치/trending-keywords?limit=8" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n\n"

# 5. 카테고리별 구독자 통계 조회
echo "5. 카테고리별 구독자 통계 조회..."
curl -X GET "${BASE_URL}/api/newsletter/category/정치/subscribers" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n\n"

# 6. 전체 트렌드 키워드 조회
echo "6. 전체 트렌드 키워드 조회..."
curl -X GET "${BASE_URL}/api/newsletter/trending-keywords?limit=10" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n\n"

# 7. 전체 카테고리별 구독자 통계 조회
echo "7. 전체 카테고리별 구독자 통계 조회..."
curl -X GET "${BASE_URL}/api/newsletter/categories/subscribers" \
  -H "Content-Type: application/json" \
  -w "\nHTTP Status: %{http_code}\n\n"

echo "=== 테스트 완료 ==="
echo "참고: 인증이 필요한 API는 실제 토큰이 필요합니다."
