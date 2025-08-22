package com.newnormallist.newsservice.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;

import com.newnormallist.newsservice.recommendation.entity.NewsScrap;

// 최근 30일 스크랩 기록 조회 -> S(c) 계산에 사용
public interface NewsScrapRepository extends JpaRepository<NewsScrap, Long> {
    @Query("SELECT ns FROM NewsScrap ns JOIN ScrapStorage ss ON ns.storageId = ss.storageId WHERE ss.userId = :uid AND ns.createdAt >= :since")
    List<NewsScrap> findRecentScrapsByUserId(@Param("uid") Long userId, @Param("since") Instant since);
}