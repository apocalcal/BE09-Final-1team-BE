package com.newnormallist.userservice.user.controller;

import com.newnormallist.userservice.common.ApiResponse;
import com.newnormallist.userservice.user.dto.MyPageResponse;
import com.newnormallist.userservice.user.dto.SignupRequest;
import com.newnormallist.userservice.user.dto.UserUpdateRequest;
import com.newnormallist.userservice.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {
    /**
     * 회원가입 API
     * @return 회원가입 성공 메시지
     */
    private final UserService userService;
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<String>> signup(@Valid @RequestBody SignupRequest signupRequest) {
        userService.signup(signupRequest);
        return ResponseEntity.ok(ApiResponse.success("회원가입이 성공적으로 완료되었습니다."));
    }

    /**
     * 마이페이지 정보 조회 API
     * @return 마이페이지 정보
     * */
    @GetMapping("/mypage")
    public ResponseEntity<ApiResponse<MyPageResponse>> getMyPage(@AuthenticationPrincipal String userIdStr) {
        Long userId = Long.parseLong(userIdStr);
        MyPageResponse myPageResponse = userService.getMyPage(userId);
        return ResponseEntity.ok(ApiResponse.success(myPageResponse));
    }

    /**
     * 마이페이지 정보 수정 API
     * @return 마이페이지 정보 수정 성공 메시지
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
     * @return 회원 탈퇴 성공 메시지
     */
    @DeleteMapping("/delete")
    public ResponseEntity<ApiResponse<String>> deleteUser(@AuthenticationPrincipal String userIdStr) {
        Long userId = Long.parseLong(userIdStr);
        userService.deleteUser(userId);
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴가 성공적으로 완료되었습니다."));
    }

}
