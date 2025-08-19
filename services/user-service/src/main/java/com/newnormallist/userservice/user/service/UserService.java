package com.newnormallist.userservice.user.service;

import com.newnormallist.userservice.auth.repository.RefreshTokenRepository;
import com.newnormallist.userservice.clients.NewsServiceClient;
import com.newnormallist.userservice.common.ErrorCode;
import com.newnormallist.userservice.history.dto.ReadHistoryResponse;
import com.newnormallist.userservice.history.entity.UserReadHistory;
import com.newnormallist.userservice.history.repository.UserReadHistoryRepository;
import com.newnormallist.userservice.user.dto.*;
import com.newnormallist.userservice.user.entity.NewsCategory;
import com.newnormallist.userservice.user.entity.User;
import com.newnormallist.userservice.common.exception.UserException;
import com.newnormallist.userservice.user.entity.UserStatus;
import com.newnormallist.userservice.user.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserReadHistoryRepository userReadHistoryRepository;
    private final NewsServiceClient newsServiceClient; // Feign Client 의존성 주입

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
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));

        // 2. 리프레시 토큰 삭제
        refreshTokenRepository.deleteByUserId(userId);

        // 3. 상태를 DELETED로 변경 (Soft Delete)
        user.changeStatus(UserStatus.DELETED);
        // userRepository.delete(user); <-- 이 코드를 위 코드로 대체

        log.info("사용자 탈퇴 처리 완료 - 사용자 ID: {}", userId);;
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
    /**
     * 관리자용 사용자 정보 조회 로직
     * @param status 필터링할 회원 상태
     * @param keyword 검색 키워드 (이메일 또는 이름)
     * @param pageable 페이지 정보
     * @return Page<UserAdminResponse> 페이징 처리된 회원 목록
     */
    @Transactional(readOnly = true)
    public Page<UserAdminResponse> getUsersForAdmin(UserStatus status, String keyword, Pageable pageable) {
        // 1. Specification을 사용한 동적 쿼리 생성
        Specification<User> spec = (root, query, criteriaBuilder) -> {
            Predicate predicate = criteriaBuilder.conjunction(); // 기본적으로 AND 조건으로 시작
            // 1-1. status 필터링 조건
            if (status != null) {
                predicate = criteriaBuilder.and(predicate, criteriaBuilder.equal(root.get("status"), status));
            }
            // 1-2. keyword 검색 조건 (이메일 또는 이름)
            if (keyword != null && !keyword.isBlank()) {
                Predicate emailLike = criteriaBuilder.like(root.get("email"), "%" + keyword + "%");
                Predicate nameLike = criteriaBuilder.like(root.get("name"), "%" + keyword + "%");
                predicate= criteriaBuilder.and(predicate, criteriaBuilder.or(emailLike, nameLike));
            }
            return predicate;
        };
        // 2. 페이징과 Specification을 적용하여 데이터 조회
        Page<User> users = userRepository.findAll(spec, pageable);
        // 3. Page<UserAdminResponse>로 변환
        return users.map(UserAdminResponse::new);
    }
    /**
     * 관리자용 사용자 정보 삭제 로직
     * @param userId 삭제할 사용자 ID
     */
    @Transactional
    public void adminHardDeleteUser(Long userId) {
        int affected = userRepository.hardDeleteIFDeleted(userId);
        if (affected == 1) {
            log.info("관리자에 의한 사용자 하드 삭제 완료 - 사용자 ID: {}", userId);
            return;
        }
        // 실패한 경우
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
        if (user.getStatus() != UserStatus.DELETED) {
            throw new UserException(ErrorCode.INVALID_STATUS);
        }
        throw new UserException(ErrorCode.OPERATION_FAILED);
    }
    /**
     * 관리자용 배치 하드 삭제 로직
     * @param before 삭제 기준 날짜 (이 날짜 이전의 사용자)
     * @return int 삭제된 사용자 수
     */
    @Transactional
    public int adminPurgeDeleted(LocalDateTime before) {
        // 1. 상태가 DELETED인 사용자 중, updatedAt이 before보다 이전인 사용자 삭제
        int deletedCount = userRepository.deleteByStatusBefore(UserStatus.DELETED, before);
        log.info("관리자에 의한 배치 하드 삭제 완료 - 삭제된 사용자 수: {}, before = {}", deletedCount, before);
        return deletedCount;
    }
    /**
     * 뉴스 읽음 기록 추가 로직
     * @param userId 사용자 ID
     * @param newsId 읽은 뉴스 ID
     * */
    @Transactional
    public void addReadHistory(Long userId, Long newsId) {
        // 이미 읽은 기록이 있는지 확인
        synchronized (this) { // 동기화 블록으로 중복 방지
            Optional<UserReadHistory> existingHistory = userReadHistoryRepository.findByUser_IdAndNewsId(userId, newsId);

            if (existingHistory.isPresent()) {
                // 기존 기록이 있으면 updated_at만 갱신
                existingHistory.get().updateReadTime();
                userReadHistoryRepository.save(existingHistory.get());
                log.info("뉴스 읽음 기록 갱신 완료 - 사용자 ID: {}, 뉴스 ID: {}", userId, newsId);
                return;
            }

            // 기록을 저장하기 위해 User 엔티티 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new UserException(ErrorCode.USER_NOT_FOUND));
            // 읽은 기록 엔티티 생성
            UserReadHistory history = UserReadHistory.builder()
                    .user(user)
                    .newsId(newsId)
                    .build();
            // 읽은 기록 저장
            userReadHistoryRepository.save(history);
            log.info("뉴스 읽음 기록 추가 완료 - 사용자 ID: {}, 뉴스 ID: {}", userId, newsId);
        }
    }
    /**
     * 사용자가 읽은 뉴스 기록 조회 로직 (updated_at 포함)
     * @param userId 사용자 ID
     * @param pageable 페이징 정보
     * @return Page<ReadHistoryResponse> 읽은 뉴스 기록 목록 (updated_at 포함)
     */
    @Transactional(readOnly = true)
    public Page<ReadHistoryResponse> getReadHistory(Long userId, Pageable pageable) {
        // updated_at 기준 내림차순으로 정렬된 기록 조회 후 DTO로 변환
        return userReadHistoryRepository.findByUserIdOrderByUpdatedAtDesc(userId, pageable)
                .map(ReadHistoryResponse::new);
    }

    /**
     * 스크랩된 뉴스 목록 조회 로직
     * @param userId 사용자 ID
     * @param pageable 페이지 정보
     * @return Page<ScrappedNewsResponse> 스크랩된 뉴스 목록
     */
    public Page<ScrappedNewsResponse> getScrappedNews(Long userId, Pageable pageable) {
        String sort = pageable.getSort().stream()
                .map(order -> order.getProperty() + "," + order.getDirection())
                .collect(Collectors.joining());
        return newsServiceClient.getScrappedNews(
                userId,
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort
        );
    }
}
