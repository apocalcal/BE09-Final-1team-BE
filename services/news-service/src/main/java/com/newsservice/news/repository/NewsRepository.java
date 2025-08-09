package com.newsservice.news.repository;

import com.newsservice.news.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    
    // 카테고리별 뉴스 조회
    @Query("SELECT n FROM News n JOIN n.originalNews nc JOIN nc.category c WHERE c.id = :categoryId")
    Page<News> findByCategoryId(@Param("categoryId") Integer categoryId, Pageable pageable);
    
    // 키워드 검색 (제목, 내용, 요약에서 검색)
    @Query("SELECT n FROM News n JOIN n.originalNews nc WHERE " +
           "nc.title LIKE %:keyword% OR nc.content LIKE %:keyword% OR n.summary LIKE %:keyword%")
    Page<News> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    // 최신 뉴스 조회 (발행일 기준 내림차순)
    @Query("SELECT n FROM News n ORDER BY n.publishedAt DESC")
    Page<News> findLatestNews(Pageable pageable);
    
    // 인기 뉴스 조회 (신뢰도 기준 내림차순)
    @Query("SELECT n FROM News n ORDER BY n.trusted DESC")
    Page<News> findPopularNews(Pageable pageable);
    
    // 트렌딩 뉴스 조회 (신뢰도 + 조회수 기준)
    @Query("SELECT n FROM News n ORDER BY n.trusted DESC, n.publishedAt DESC")
    Page<News> findTrendingNews(Pageable pageable);
    
    // 특정 기간 내 뉴스 조회
    @Query("SELECT n FROM News n WHERE n.publishedAt BETWEEN :startDate AND :endDate")
    Page<News> findByPublishedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate, 
                                       Pageable pageable);
    
    // 신뢰도가 높은 뉴스 조회
    @Query("SELECT n FROM News n WHERE n.trusted >= :minTrusted")
    Page<News> findByTrustedGreaterThanEqual(@Param("minTrusted") Integer minTrusted, Pageable pageable);
    
    // 원본 뉴스 ID로 조회
    List<News> findByOriginalNewsId(Long originalNewsId);
    
    // 요약이 있는 뉴스만 조회
    @Query("SELECT n FROM News n WHERE n.summary IS NOT NULL AND n.summary != ''")
    Page<News> findWithSummary(Pageable pageable);
    
    // 특정 언론사 뉴스 조회
    @Query("SELECT n FROM News n JOIN n.originalNews nc WHERE nc.press = :press")
    Page<News> findByPress(@Param("press") String press, Pageable pageable);
    
    // 전체 뉴스 조회 (페이징)
    Page<News> findAll(Pageable pageable);
} 