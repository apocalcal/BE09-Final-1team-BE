package com.newnormallist.userservice.user.controller;

import com.newnormallist.userservice.common.ApiResponse;
import com.newnormallist.userservice.user.dto.*;
import com.newnormallist.userservice.user.entity.UserStatus;
import com.newnormallist.userservice.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 회원가입 API
     */
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        userService.signup(signupRequest);
        return ResponseEntity.ok(ApiResponse.success("회원가입이 성공적으로 완료되었습니다."));
    }

    /**
     * 마이페이지 (내 정보) 조회 API
     */
    @GetMapping("/mypage")
    public ResponseEntity<ApiResponse<MyPageResponse>> getMyPage(@AuthenticationPrincipal String userIdStr) {
        Long userId = Long.parseLong(userIdStr);
        MyPageResponse myPageResponse = userService.getMyPage(userId);
        return ResponseEntity.ok(ApiResponse.success(myPageResponse));
    }

    /**
     * 마이페이지 정보 수정 API
     */
    @PutMapping("/myupdate")
    public ResponseEntity<ApiResponse<String>> updateMyPage(
            @AuthenticationPrincipal String userIdStr,
            @Valid @RequestBody UserUpdateRequest userUpdateRequest
    ) {
        Long userId = Long.parseLong(userIdStr);
        userService.updateMyPage(userId, userUpdateRequest);
        return ResponseEntity.ok(ApiResponse.success("마이페이지 정보가 성공적으로 수정되었습니다."));
    }

    /**
     * 회원 탈퇴 API
     */
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<String>> deleteUser(@AuthenticationPrincipal String userIdStr) {
        Long userId = Long.parseLong(userIdStr);
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 성공적으로 완료되었습니다."));
    }

    /**
     * 관심사 목록 가져오기 API
     */
    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getNewsCategories() {
        List<CategoryResponse> categories = userService.getNewsCategories();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }
    /**
     * 관리자용 회원 목록 조회 API
     */
    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<UserAdminResponse>>> getUsers(
            @RequestParam(required = false)UserStatus status,
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)Pageable pageable
            ) {
        Page<UserAdminResponse> userPage = userService.getUsersForAdmin(status, keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(userPage));
    }
    /**
     * 하드 삭제 API
     */
    @DeleteMapping("/internal/admin/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> hardDelete(@PathVariable Long userId) {
        userService.adminHardDeleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    /**
     * 관리자용 배치 하드 삭제 API
     */
    @DeleteMapping("/internal/admin/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> purge(
            @RequestParam("before")
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime before) {
        int deleted = userService.adminPurgeDeleted(before);
        return ResponseEntity.ok(ApiResponse.success(Map.of("deleted", deleted, "before", before.toString())));
    }
    /**
     * 뉴스 읽음 기록 추가 API
     * */
    @PostMapping("/mypage/history/{newsId}")
    public ResponseEntity<ApiResponse<String>> addReadHistory(
            @AuthenticationPrincipal String userIdStr,
            @PathVariable Long newsId) {
        Long userId = Long.parseLong(userIdStr);
        userService.addReadHistory(userId, newsId);
        return ResponseEntity.ok(ApiResponse.success("읽은 뉴스 목록에 추가됨!"));
    }
}
