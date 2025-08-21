package com.newnormallist.newsservice.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

import com.newnormallist.newsservice.recommendation.entity.NewsScrap;

// 최근 30일 스크랩 기록 조회 -> S(c) 계산에 사용
public interface NewsScrapRepository extends JpaRepository<NewsScrap, Long> {
    @Query("select s from NewsScrap s where s.userId = :uid and s.createdAt >= :since")
    List<NewsScrap> findRecentScrapsAcrossAllBoxes(@Param("uid") Long userId, @Param("since") Instant since);
}