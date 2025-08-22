package com.newsletterservice.client;



import com.newsletterservice.client.dto.CategoryResponse;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.client.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(
        name = "user-service-client", 
        url = "${services.user-service.url:http://localhost:8081}",
        contextId = "newsletterUserServiceClient")
public interface UserServiceClient {
    
    /**
     * 사용자 정보 조회
     */
    @GetMapping("/api/users/{userId}")
    ApiResponse<UserResponse> getUserById(@PathVariable("userId") Long userId);
    
    /**
     * 사용자 이메일로 정보 조회
     */
    @GetMapping("/api/users/email/{email}")
    ApiResponse<UserResponse> getUserByEmail(@PathVariable("email") String email);
    
    /**
     * 여러 사용자 정보 일괄 조회
     */
    @PostMapping("/api/users/batch")
    ApiResponse<List<UserResponse>> getUsersByIds(@RequestBody List<Long> userIds);
    
    /**
     * 사용자 선호 카테고리 조회
     */
    @GetMapping("/api/users/{userId}/categories")
    ApiResponse<List<CategoryResponse>> getUserPreferences(@PathVariable("userId") Long userId);
    
    /**
     * 활성 사용자 목록 조회 (뉴스레터 발송용)
     */
    @GetMapping("/api/users/active")
    ApiResponse<List<UserResponse>> getActiveUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    );
}
