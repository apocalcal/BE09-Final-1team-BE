package com.newsservice.user.controller;

import com.newsservice.user.dto.LoginRequest;
import com.newsservice.user.dto.SignupRequest;
import com.newsservice.user.dto.UserResponse;
import com.newsservice.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {
    
    @Autowired
    private UserService userService;
    
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest signupRequest) {
        try {
            UserResponse user = userService.signup(signupRequest);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("회원가입 실패: " + e.getMessage());
        }
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest) {
        try {
            String token = userService.login(loginRequest);
            return ResponseEntity.ok().body(new LoginResponse(token));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("로그인 실패: " + e.getMessage());
        }
    }
    
    @GetMapping("/profile")
    public ResponseEntity<UserResponse> getProfile(@RequestHeader("X-User-Id") String userId) {
        UserResponse user = userService.getUserProfile(Long.parseLong(userId));
        return ResponseEntity.ok(user);
    }
    
    @PutMapping("/profile")
    public ResponseEntity<UserResponse> updateProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse user = userService.updateProfile(Long.parseLong(userId), request);
        return ResponseEntity.ok(user);
    }
    
    @DeleteMapping("/profile")
    public ResponseEntity<?> deleteAccount(@RequestHeader("X-User-Id") String userId) {
        userService.deleteUser(Long.parseLong(userId));
        return ResponseEntity.ok().body("회원 탈퇴가 완료되었습니다.");
    }
    
    // Inner classes for responses
    public static class LoginResponse {
        private String token;
        
        public LoginResponse(String token) {
            this.token = token;
        }
        
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
    }
}
