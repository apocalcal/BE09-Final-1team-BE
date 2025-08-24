#!/bin/bash

echo "🧪 Newsletter Service 호환성 스모크 테스트"
echo "=========================================="

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# 테스트 함수
test_endpoint() {
    local name="$1"
    local url="$2"
    local expected_status="$3"
    
    echo -n "테스트: $name... "
    
    response=$(curl -s -w "%{http_code}" -o /tmp/response.json "$url")
    status_code="${response: -3}"
    
    if [ "$status_code" = "$expected_status" ]; then
        echo -e "${GREEN}✅ 성공 (HTTP $status_code)${NC}"
        return 0
    else
        echo -e "${RED}❌ 실패 (HTTP $status_code, 예상: $expected_status)${NC}"
        if [ -f /tmp/response.json ]; then
            echo "응답: $(cat /tmp/response.json)"
        fi
        return 1
    fi
}

# 테스트 결과 카운터
passed=0
failed=0

echo ""
echo "📰 News Service 테스트 (포트 8082)"
echo "--------------------------------"

# News Service 테스트
if test_endpoint "카테고리 목록" "http://localhost:8082/api/news/categories" "200"; then
    ((passed++))
else
    ((failed++))
fi

if test_endpoint "최신 뉴스" "http://localhost:8082/api/news/latest?limit=5" "200"; then
    ((passed++))
else
    ((failed++))
fi

if test_endpoint "인기 뉴스" "http://localhost:8082/api/news/popular?size=5" "200"; then
    ((passed++))
else
    ((failed++))
fi

echo ""
echo "👤 User Service 테스트 (포트 8081)"
echo "--------------------------------"

# User Service 테스트
if test_endpoint "사용자 정보" "http://localhost:8081/api/users/1" "200"; then
    ((passed++))
else
    ((failed++))
fi

if test_endpoint "사용자 카테고리" "http://localhost:8081/api/users/1/categories" "200"; then
    ((passed++))
else
    ((failed++))
fi

if test_endpoint "활성 사용자" "http://localhost:8081/api/users/active?size=5" "200"; then
    ((passed++))
else
    ((failed++))
fi

echo ""
echo "📧 Newsletter Service 테스트 (포트 8085)"
echo "--------------------------------------"

# Newsletter Service 테스트
if test_endpoint "헬스 체크" "http://localhost:8085/actuator/health" "200"; then
    ((passed++))
else
    ((failed++))
fi

if test_endpoint "대시보드" "http://localhost:8085/api/newsletter/dashboard?userId=1" "200"; then
    ((passed++))
else
    ((failed++))
fi

if test_endpoint "구독 정보" "http://localhost:8085/api/newsletter/subscription?userId=1" "200"; then
    ((passed++))
else
    ((failed++))
fi

echo ""
echo "📊 테스트 결과"
echo "=============="
echo -e "${GREEN}✅ 성공: $passed${NC}"
echo -e "${RED}❌ 실패: $failed${NC}"
echo "총 테스트: $((passed + failed))"

if [ $failed -eq 0 ]; then
    echo -e "${GREEN}🎉 모든 테스트 통과! Newsletter Service가 완전 호환 상태입니다.${NC}"
    exit 0
else
    echo -e "${RED}⚠️  일부 테스트 실패. 호환성 문제가 있을 수 있습니다.${NC}"
    exit 1
fi
