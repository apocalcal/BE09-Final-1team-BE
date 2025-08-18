package com.newsletterservice.controller;

import com.newsletterservice.client.NewsServiceClient;
import com.newsletterservice.client.UserServiceClient;
import com.newsletterservice.client.dto.CategoryResponse;
import com.newsletterservice.client.dto.NewsResponse;
import com.newsletterservice.client.dto.UserResponse;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.entity.NewsCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestDataController {

    private final NewsServiceClient newsServiceClient;
    private final UserServiceClient userServiceClient;

    /**
     * 뉴스 서비스에서 최신 뉴스 데이터 조회
     */
    @GetMapping("/news/latest")
    public ApiResponse<List<NewsResponse>> getLatestNews(
            @RequestParam(required = false) List<String> categoryName,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("최신 뉴스 조회 요청 - 카테고리: {}, 개수: {}", categoryName, limit);
            ApiResponse<List<NewsResponse>> response = newsServiceClient.getLatestNews(categoryName, limit);
            log.info("최신 뉴스 조회 성공 - 뉴스 개수: {}", 
                response.getData() != null ? response.getData().size() : 0);
            return response;
        } catch (Exception e) {
            log.error("최신 뉴스 조회 실패", e);
            return ApiResponse.error("NEWS_FETCH_ERROR", "뉴스 데이터 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 특정 카테고리의 뉴스 조회
     */
    @GetMapping("/news/category/{categoryName}")
    public ApiResponse<List<NewsResponse>> getNewsByCategory(
            @PathVariable String categoryName,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {
        try {
            log.info("카테고리별 뉴스 조회 요청 - 카테고리: {}, 페이지: {}, 크기: {}", categoryName, page, size);
            ApiResponse<List<NewsResponse>> response = newsServiceClient.getNewsByCategory(categoryName, page, size);
            log.info("카테고리별 뉴스 조회 성공 - 뉴스 개수: {}", 
                response.getData() != null ? response.getData().size() : 0);
            return response;
        } catch (Exception e) {
            log.error("카테고리별 뉴스 조회 실패 - 카테고리: {}", categoryName, e);
            return ApiResponse.error("CATEGORY_NEWS_FETCH_ERROR", "카테고리별 뉴스 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 트렌딩 뉴스 조회
     */
    @GetMapping("/news/trending")
    public ApiResponse<List<NewsResponse>> getTrendingNews(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "5") int limit) {
        try {
            log.info("트렌딩 뉴스 조회 요청 - 시간: {}시간, 개수: {}", hours, limit);
            ApiResponse<List<NewsResponse>> response = newsServiceClient.getTrendingNews(hours, limit);
            log.info("트렌딩 뉴스 조회 성공 - 뉴스 개수: {}", 
                response.getData() != null ? response.getData().size() : 0);
            return response;
        } catch (Exception e) {
            log.error("트렌딩 뉴스 조회 실패", e);
            return ApiResponse.error("TRENDING_NEWS_FETCH_ERROR", "트렌딩 뉴스 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 사용자 서비스에서 사용자 정보 조회
     */
    @GetMapping("/users/{userId}")
    public ApiResponse<UserResponse> getUserById(@PathVariable Long userId) {
        try {
            log.info("사용자 정보 조회 요청 - 사용자 ID: {}", userId);
            ApiResponse<UserResponse> response = userServiceClient.getUserById(userId);
            log.info("사용자 정보 조회 성공 - 사용자: {}", 
                response.getData() != null ? response.getData().getEmail() : "null");
            return response;
        } catch (Exception e) {
            log.error("사용자 정보 조회 실패 - 사용자 ID: {}", userId, e);
            return ApiResponse.error("USER_FETCH_ERROR", "사용자 정보 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 활성 사용자 목록 조회
     */
    @GetMapping("/users/active")
    public ApiResponse<List<UserResponse>> getActiveUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            log.info("활성 사용자 목록 조회 요청 - 페이지: {}, 크기: {}", page, size);
            ApiResponse<List<UserResponse>> response = userServiceClient.getActiveUsers(page, size);
            log.info("활성 사용자 목록 조회 성공 - 사용자 수: {}", 
                response.getData() != null ? response.getData().size() : 0);
            return response;
        } catch (Exception e) {
            log.error("활성 사용자 목록 조회 실패", e);
            return ApiResponse.error("ACTIVE_USERS_FETCH_ERROR", "활성 사용자 목록 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 사용자 선호 카테고리 조회
     */
    @GetMapping("/users/{userId}/categories")
    public ApiResponse<List<CategoryResponse>> getUserPreferences(@PathVariable Long userId) {
        try {
            log.info("사용자 선호 카테고리 조회 요청 - 사용자 ID: {}", userId);
            ApiResponse<List<CategoryResponse>> response = userServiceClient.getUserPreferences(userId);
            log.info("사용자 선호 카테고리 조회 성공 - 카테고리 수: {}", 
                response.getData() != null ? response.getData().size() : 0);
            return response;
        } catch (Exception e) {
            log.error("사용자 선호 카테고리 조회 실패 - 사용자 ID: {}", userId, e);
            return ApiResponse.error("USER_PREFERENCES_FETCH_ERROR", "사용자 선호 카테고리 조회 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 뉴스레터 콘텐츠 생성 테스트
     */
    @GetMapping("/newsletter/content")
    public String testNewsletterContent() {
        try {
            log.info("뉴스레터 콘텐츠 생성 테스트 시작");
            
            // 최신 뉴스 가져오기
            ApiResponse<List<NewsResponse>> latestNewsResponse = newsServiceClient.getLatestNews(null, 3);
            List<NewsResponse> latestNews = latestNewsResponse.getData();
            
            StringBuilder content = new StringBuilder();
            content.append("<h1>📧 뉴스레터 테스트</h1>\n");
            content.append("<p>실제 백엔드 데이터로 생성된 뉴스레터입니다.</p>\n\n");
            
            if (latestNews != null && !latestNews.isEmpty()) {
                content.append("<h2>📰 최신 뉴스</h2>\n");
                for (NewsResponse news : latestNews) {
                    content.append("<div style='margin-bottom: 20px; padding: 15px; border: 1px solid #eee; border-radius: 8px;'>\n");
                    content.append("<h3>").append(news.getTitle()).append("</h3>\n");
                    content.append("<p><strong>카테고리:</strong> ").append(news.getCategory()).append("</p>\n");
                    if (news.getSummary() != null && !news.getSummary().isEmpty()) {
                        content.append("<p>").append(news.getSummary()).append("</p>\n");
                    }
                    content.append("<p><small>작성일: ").append(news.getCreatedAt()).append("</small></p>\n");
                    content.append("</div>\n");
                }
            } else {
                content.append("<p>현재 뉴스 데이터가 없습니다.</p>\n");
            }
            
            content.append("<p>이 뉴스레터는 실제 백엔드 API에서 가져온 데이터로 생성되었습니다.</p>\n");
            
            log.info("뉴스레터 콘텐츠 생성 테스트 완료");
            return content.toString();
            
        } catch (Exception e) {
            log.error("뉴스레터 콘텐츠 생성 테스트 실패", e);
            return "<h1>오류 발생</h1><p>뉴스레터 콘텐츠 생성 중 오류가 발생했습니다: " + e.getMessage() + "</p>";
        }
    }

    /**
     * 서비스 연결 상태 확인
     */
    @GetMapping("/health")
    public String checkServiceHealth() {
        StringBuilder status = new StringBuilder();
        status.append("<h1>🔍 서비스 연결 상태</h1>\n");
        
        // 뉴스 서비스 연결 확인
        try {
            ApiResponse<List<NewsResponse>> newsResponse = newsServiceClient.getLatestNews(null, 1);
            status.append("<p>✅ 뉴스 서비스: 연결됨</p>\n");
        } catch (Exception e) {
            status.append("<p>❌ 뉴스 서비스: 연결 실패 - ").append(e.getMessage()).append("</p>\n");
        }
        
        // 사용자 서비스 연결 확인
        try {
            ApiResponse<List<UserResponse>> userResponse = userServiceClient.getActiveUsers(0, 1);
            status.append("<p>✅ 사용자 서비스: 연결됨</p>\n");
        } catch (Exception e) {
            status.append("<p>❌ 사용자 서비스: 연결 실패 - ").append(e.getMessage()).append("</p>\n");
        }
        
        return status.toString();
    }
}
