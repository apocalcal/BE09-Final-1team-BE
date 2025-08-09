package com.newnormallist.userservice.user.controller;

import com.newnormallist.userservice.common.ApiResponse;
import com.newnormallist.userservice.user.dto.CategoryResponse;
import com.newnormallist.userservice.user.dto.MyPageResponse;
import com.newnormallist.userservice.user.dto.SignupRequest;
import com.newnormallist.userservice.user.dto.UserUpdateRequest;
import com.newnormallist.userservice.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
     * 마이페이지 정보 조회 API
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
}
