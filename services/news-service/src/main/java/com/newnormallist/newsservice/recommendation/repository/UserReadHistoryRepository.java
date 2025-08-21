package com.newnormallist.newsservice.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import com.newnormallist.newsservice.recommendation.entity.UserReadHistory;

// 최근 7일 조회 로그 조회 -> R(c) 계산에 사용
public interface UserReadHistoryRepository extends JpaRepository<UserReadHistory, Long> {
    List<UserReadHistory> findByUserId(Long userId);
}