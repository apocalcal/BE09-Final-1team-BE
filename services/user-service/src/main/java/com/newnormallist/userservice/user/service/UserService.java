package com.newnormallist.userservice.user.service;

import com.newnormallist.userservice.auth.repository.RefreshTokenRepository;
import com.newnormallist.userservice.common.ErrorCode;
import com.newnormallist.userservice.user.dto.CategoryResponse;
import com.newnormallist.userservice.user.dto.MyPageResponse;
import com.newnormallist.userservice.user.dto.SignupRequest;
import com.newnormallist.userservice.user.dto.UserUpdateRequest;
import com.newnormallist.userservice.user.entity.NewsCategory;
import com.newnormallist.userservice.user.entity.User;
import com.newnormallist.userservice.common.exception.UserException;
import com.newnormallist.userservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입 로직
     * @param signupRequest 회원가입 요청 정보
     */
    @Transactional
    public void signup(SignupRequest signupRequest) {
        // 1. 이메일 중복 검사
        validateEmailDuplication(signupRequest.getEmail());
        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(signupRequest.getPassword());
        // 3. User 엔티티 생성
        User user = User.builder()
                .email(signupRequest.getEmail())
                .password(encodedPassword)
                .name(signupRequest.getName())
                .birthYear(signupRequest.getBirthYear())
                .gender(signupRequest.getGender())
                .hobbies(signupRequest.getHobbies() != null ? signupRequest.getHobbies() : new HashSet<>())
                .build();
        // 4. 사용자 저장
        userRepository.save(user);
        log.info("사용자 회원가입 완료 - 이메일: {}", signupRequest.getEmail());
    }
    private void validateEmailDuplication(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new UserException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }
    }

    /**
     * 마이페이지 정보 조회 로직
     * @param userId 현재 인증된 사용자 ID
     * @return MyPageResponse 마이페이지 정보
     */
    @Transactional(readOnly = true)
    public MyPageResponse getMyPage(Long userId) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        // 2. 조회된 사용자 정보를 DTO로 변환
        return new MyPageResponse(user);
    }
    /**
     * 마이페이지 정보 수정 로직
     * @param userId 현재 인증된 사용자 ID
     * @param request 수정할 정보
     */
    @Transactional
    public void updateMyPage(Long userId, UserUpdateRequest request) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        // 2. 비밀번호 변경 로직
        // newPassword 필드가 비어있지 않은 경우에만 실행
        if (request.getNewPassword() != null && !request.getNewPassword().isBlank()) {
            // 2-1. 현재 비밀번호 확인
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new UserException(ErrorCode.CURRENT_PASSWORD_REQUIRED);
            }
            // 2-2. 현재 비밀번호가 올바른지 검증
            if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
                throw new UserException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
            }
            // 2-3. 새 비밀번호와 확인 비밀번호 일치 여부 확인
            if (!request.getNewPassword().equals(request.getConfirmPassword())) {
                throw new UserException(ErrorCode.PASSWORD_MISMATCH);
            }
            // 2-4. 비밀번호 암호화 및 업데이트
            String encodedPassword = passwordEncoder.encode(request.getNewPassword());
            user.updatePassword(encodedPassword);
        }
        // 3. 뉴스레터 수신 여부 및 관심사 업데이트
        user.updateProfile(request.getLetterOk(), request.getHobbies());
        log.info("사용자 마이페이지 정보 수정 완료 - 사용자 ID: {}", userId);
    }
    /**
     * 회원 탈퇴 로직
     * @param userId 현재 인증된 사용자 ID
     */
    @Transactional
    public void deleteUser(Long userId) {
        // 1. 사용자 ID로 refreshtoken 삭제
        refreshTokenRepository.deleteByUserId(userId);
        // 2. 사용자 존재 여부 조회
        if (!userRepository.existsById(userId)) {
            throw new UserException(ErrorCode.USER_NOT_FOUND);
        }
        // 3. 사용자 삭제
        userRepository.deleteById(userId);
        log.info("사용자 탈퇴 완료 - 사용자 ID: {}", userId);
    }
    /**
     * 뉴스 카테고리 목록 조회 로직
     * @return List<CategoryResponse> 뉴스 카테고리 목록
     * */
    public List<CategoryResponse> getNewsCategories() {
        List<CategoryResponse> categories = Arrays.stream(NewsCategory.values())
                .map(category -> new CategoryResponse(
                        category.name(),
                        category.getCategoryName(),
                        category.getIcon()))
                .toList();
        log.info("뉴스 카테고리 목록 조회 완료 - 카테고리 수: {}", categories.size());
        return categories;
    }
}
