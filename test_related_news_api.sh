#!/bin/bash

# 연관뉴스 API 테스트 스크립트

BASE_URL="http://localhost:8083/api/news"

echo "=== 연관뉴스 API 테스트 ==="

# 테스트할 뉴스 ID (실제 존재하는 뉴스 ID로 변경 필요)
NEWS_ID=1

echo "1. 뉴스 ID ${NEWS_ID}의 연관뉴스 조회"
echo "URL: ${BASE_URL}/${NEWS_ID}/related"
echo ""

response=$(curl -s -w "\nHTTP_STATUS:%{http_code}" "${BASE_URL}/${NEWS_ID}/related")

# HTTP 상태 코드와 응답 본문 분리
http_status=$(echo "$response" | grep "HTTP_STATUS:" | cut -d: -f2)
response_body=$(echo "$response" | sed '/HTTP_STATUS:/d')

echo "HTTP Status: $http_status"
echo "Response:"
echo "$response_body" | jq '.' 2>/dev/null || echo "$response_body"

echo ""
echo "=== 테스트 완료 ==="
