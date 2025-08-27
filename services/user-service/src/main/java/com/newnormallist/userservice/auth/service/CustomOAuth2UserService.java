package com.newnormallist.userservice.auth.service;

import com.newnormallist.userservice.user.entity.User;
import com.newnormallist.userservice.user.entity.UserRole;
import com.newnormallist.userservice.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        // 1. 기본 OAuth2UserService 객체 생성
        OAuth2UserService<OAuth2UserRequest, OAuth2User> oAuth2UserService = new DefaultOAuth2UserService();

        // 2. userRequest를 통해 카카오에서 사용자 정보 로드
        OAuth2User oAuth2User = oAuth2UserService.loadUser(userRequest);

        // 3. 클라이언트 등록 ID와 사용자 이름 속성(id)을 가져옴
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        // 4. 카카오 정보 파싱을 위한 DTO 객체 생성 (우선은 Map 형태로 받음)
        Map<String, Object> attributes = oAuth2User.getAttributes();
        // 카카오 응답에서 필요한 정보 추출
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");

        String email = (String) kakaoAccount.get("email");
        String nickname = (String) profile.get("nickname");
        String providerId = attributes.get("id").toString();

        // 5. 이메일을 기준으로 우리 DB에서 사용자 조회
        User user = saveOrUpdate(email, nickname, providerId);

        // 6. Spring Security가 사용할 OAuth2User 반환
        return new DefaultOAuth2User(
                Collections.singleton(new SimpleGrantedAuthority(user.getRole().name())),
                attributes,
                userNameAttributeName
        );
    }

    private User saveOrUpdate(String email, String nickname, String providerId) {
        // 이메일로 사용자 조회
        Optional<User> userOptional = userRepository.findByEmail(email);
        User user;
        if (userOptional.isPresent()) {
            // 이미 가입된 회원이면 정보 업데이트 (이름 등)
            user = userOptional.get();
            user.updateSocialInfo(nickname, "kakao", providerId);
        } else {
            // 가입된 회원이 아니면 새로 생성
            String randomPassword = passwordEncoder.encode(UUID.randomUUID().toString());
            user = User.builder()
                    .email(email)
                    .name(nickname)
                    .password(randomPassword) // 랜덤 비밀번호 설정
                    .provider("kakao")
                    .providerId(providerId)
                    .role(UserRole.USER) // 기본 권한 설정
                    .build();
            userRepository.save(user);
        }
        return user;
    }
}
