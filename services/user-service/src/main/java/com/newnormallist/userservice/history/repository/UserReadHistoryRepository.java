package com.newnormallist.userservice.history.repository;

import com.newnormallist.userservice.history.entity.UserReadHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserReadHistoryRepository extends JpaRepository<UserReadHistory, Long> {
    // 사용자 ID와 뉴스 ID로 읽은 기록이 존재하는지 확인
    boolean existsByUser_IdAndNewsId(Long userId, Long newsId);

    // 사용자 ID와 뉴스 ID로 읽은 기록 조회
    Optional<UserReadHistory> findByUser_IdAndNewsId(Long userId, Long newsId);

    // 특정 사용자가 읽은 뉴스의 ID 목록 조회(페이징)
    @Query("SELECT h.newsId FROM UserReadHistory h WHERE h.user.id = :userId")
    Page<Long> findNewsIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    // 특정 사용자가 읽은 뉴스 기록 전체 조회(페이징) - updated_at 포함
    @Query("SELECT h FROM UserReadHistory h WHERE h.user.id = :userId ORDER BY h.updatedAt DESC")
    Page<UserReadHistory> findByUserIdOrderByUpdatedAtDesc(@Param("userId") Long userId, Pageable pageable);

}
