package com.newnormallist.newsservice.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.newnormallist.newsservice.recommendation.entity.User;

// 유저 메타 조회 (AgeBucket 계산은 서비스/유틸에서 처리 가능)
public interface UserRepository extends JpaRepository<User, Long> { }

