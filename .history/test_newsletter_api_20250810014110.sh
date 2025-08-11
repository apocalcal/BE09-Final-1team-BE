#!/bin/bash

# 뉴스레터 API 테스트 스크립트
BASE_URL="http://localhost:8082"

echo "=== 뉴스레터 API 테스트 ==="

# 1. 구독 요청
echo "1. 뉴스레터 구독 요청..."
curl -X POST "${BASE_URL}/api/newsletter/subscribe" \
  -H "Content-Type: application/json" \
  -d '{"email": "test@example.com"}' \
  -w "\nHTTP Status: %{http_code}\n\n"

# 2. 구독자 수 조회
echo "2. 구독자 수 조회..."
curl -X GET "${BASE_URL}/api/newsletter/count" \
  -w "\nHTTP Status: %{http_code}\n\n"

echo "=== 테스트 완료 ==="
echo "참고: 구독 확인은 실제 토큰이 필요합니다."
