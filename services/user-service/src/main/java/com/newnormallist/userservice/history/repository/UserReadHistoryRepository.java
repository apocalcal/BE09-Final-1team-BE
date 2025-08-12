package com.newnormallist.userservice.history.repository;

import com.newnormallist.userservice.history.entity.UserReadHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserReadHistoryRepository extends JpaRepository<UserReadHistory, Long> {
    // 사용자 ID와 뉴스 ID로 읽은 기록이 존재하는지 확인
    boolean existsByUser_IdAndNewsId(Long userId, Long newsId);
    // 특정 사용자가 읽은 뉴스의 ID 목록 조회(페이징)
//    Page<Long>

}
