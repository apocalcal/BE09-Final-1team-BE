package com.newnormallist.userservice.history.repository;

import com.newnormallist.userservice.history.entity.UserReadHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserReadHistoryRepository extends JpaRepository<UserReadHistory, Long> {
    // 사용자 ID와 뉴스 ID로 읽은 기록이 존재하는지 확인
    boolean existsByUser_IDAndNewsId(Long userId, Long newsId);
}
