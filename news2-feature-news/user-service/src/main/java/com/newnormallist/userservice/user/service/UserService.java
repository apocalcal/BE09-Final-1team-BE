package com.newnormallist.userservice.user.service;

import com.newnormallist.userservice.common.ErrorCode;
import com.newnormallist.userservice.user.dto.SignupRequest;
import com.newnormallist.userservice.user.entity.User;
import com.newnormallist.userservice.user.exception.UserException;
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
}
