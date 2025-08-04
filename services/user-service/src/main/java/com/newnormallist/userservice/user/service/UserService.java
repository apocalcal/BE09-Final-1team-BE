package com.newnormallist.userservice.user.service;

import com.newnormallist.userservice.common.ErrorCode;
import com.newnormallist.userservice.user.dto.MyPageResponse;
import com.newnormallist.userservice.user.dto.SignupRequest;
import com.newnormallist.userservice.user.dto.UserUpdateRequest;
import com.newnormallist.userservice.user.entity.User;
import com.newnormallist.userservice.common.exception.UserException;
import com.newnormallist.userservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
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
                .letterOk(signupRequest.getLetterOk())
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
     * @param userUpdateRequest 수정할 정보
     */
    @Transactional
    public void updateMyPage(Long userId, UserUpdateRequest userUpdateRequest) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        // 2. 비밀번호가 변경된 경우 암호화
        if (userUpdateRequest.getPassword() != null && !userUpdateRequest.getPassword().isBlank()) {
            String encodedPassword = passwordEncoder.encode(userUpdateRequest.getPassword());
            user.updatePassword(encodedPassword);
            // 3. 뉴스레터 수신 동의 여부 업데이트 및 관심사 업데이트
            user.updateProfile(userUpdateRequest.getLetterOk(), userUpdateRequest.getHobbies());
            log.info("사용자 마이페이지 정보 수정 완료 - 사용자 ID: {}", userId);
        } else {
            throw new UserException(ErrorCode.INVALID_INPUT_VALUE);
            }
        }
    }
