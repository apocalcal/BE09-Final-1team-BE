import requests
import json
from datetime import datetime

def test_news_crawl_preview_api():
    """크롤링된 뉴스 데이터의 미리보기를 테스트"""
    
    # 테스트용 뉴스 데이터
    news_data = {
        "title": "테스트 뉴스 제목",
        "reporter": "테스트 기자",
        "date": "2024-01-15T10:30:00",  # ISO 형식
        "link": "https://example.com/news/123",
        "press": "테스트 언론사",
        "categoryId": 1,  # 카테고리 ID
        "content": "이것은 테스트 뉴스의 본문입니다. 크롤링된 내용이 여기에 들어갑니다. 이 뉴스는 미리보기용으로만 사용되며 실제로는 저장되지 않습니다."
    }
    
    # API 엔드포인트
    api_url = "http://localhost:8082/api/news/crawl/preview"
    
    try:
        # POST 요청 전송
        response = requests.post(
            api_url,
            json=news_data,
            headers={'Content-Type': 'application/json'},
            timeout=10
        )
        
        print(f"미리보기 응답 상태 코드: {response.status_code}")
        
        if response.status_code == 200:
            preview_data = response.json()
            print("✅ 뉴스 미리보기 성공!")
            print(f"  제목: {preview_data.get('title')}")
            print(f"  요약: {preview_data.get('summary')}")
            print(f"  카테고리: {preview_data.get('category')}")
            print(f"  언론사: {preview_data.get('sourceName')}")
        else:
            print(f"❌ 미리보기 실패: {response.status_code}")
            print(f"응답 내용: {response.text}")
            
    except requests.exceptions.ConnectionError:
        print("❌ 연결 실패: news-service가 실행 중인지 확인하세요 (포트 8082)")
    except Exception as e:
        print(f"❌ 오류 발생: {e}")

def test_news_crawl_api():
    """크롤링된 뉴스 데이터를 백엔드 API로 전송하는 테스트"""
    
    # 테스트용 뉴스 데이터
    news_data = {
        "title": "테스트 뉴스 제목",
        "reporter": "테스트 기자",
        "date": "2024-01-15T10:30:00",  # ISO 형식
        "link": "https://example.com/news/123",
        "press": "테스트 언론사",
        "categoryId": 1,  # 카테고리 ID (실제 DB에 존재하는 ID여야 함)
        "content": "이것은 테스트 뉴스의 본문입니다. 크롤링된 내용이 여기에 들어갑니다."
    }
    
    # API 엔드포인트 (NewsController에 통합됨)
    api_url = "http://localhost:8082/api/news/crawl"
    
    try:
        # POST 요청 전송
        response = requests.post(
            api_url,
            json=news_data,
            headers={'Content-Type': 'application/json'},
            timeout=10
        )
        
        print(f"응답 상태 코드: {response.status_code}")
        print(f"응답 내용: {response.text}")
        
        if response.status_code == 200:
            print("✅ 뉴스 데이터 전송 성공!")
        else:
            print(f"❌ 전송 실패: {response.status_code}")
            
    except requests.exceptions.ConnectionError:
        print("❌ 연결 실패: news-service가 실행 중인지 확인하세요 (포트 8082)")
    except Exception as e:
        print(f"❌ 오류 발생: {e}")

def test_category_api():
    """카테고리 목록을 조회하는 테스트"""
    
    try:
        response = requests.get("http://localhost:8082/api/news/categories")
        print(f"카테고리 조회 결과: {response.status_code}")
        if response.status_code == 200:
            categories = response.json()
            print("사용 가능한 카테고리:")
            for cat in categories:
                print(f"  ID: {cat.get('id')}, 이름: {cat.get('name')}")
    except Exception as e:
        print(f"카테고리 조회 실패: {e}")

def test_pending_news_api():
    """승격 대기 중인 뉴스 목록을 조회하는 테스트"""
    
    try:
        response = requests.get("http://localhost:8082/api/news/pending")
        print(f"승격 대기 뉴스 조회 결과: {response.status_code}")
        if response.status_code == 200:
            pending_news = response.json()
            print(f"승격 대기 중인 뉴스 개수: {len(pending_news)}")
            for news in pending_news[:3]:  # 처음 3개만 출력
                print(f"  - {news.get('title', '제목 없음')}")
    except Exception as e:
        print(f"승격 대기 뉴스 조회 실패: {e}")

if __name__ == "__main__":
    print("=== 뉴스 크롤링 API 테스트 (중복 제거 후) ===")
    
    # 1. 카테고리 조회 테스트
    print("\n1. 카테고리 조회 테스트")
    test_category_api()
    
    # 2. 뉴스 미리보기 테스트 (새로 추가)
    print("\n2. 뉴스 미리보기 테스트")
    test_news_crawl_preview_api()
    
    # 3. 뉴스 데이터 전송 테스트
    print("\n3. 뉴스 데이터 전송 테스트")
    test_news_crawl_api()
    
    # 4. 승격 대기 뉴스 조회 테스트
    print("\n4. 승격 대기 뉴스 조회 테스트")
    test_pending_news_api() 