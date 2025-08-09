package com.newnormallist.newsservice.repository;

import com.newnormallist.newsservice.entity.News;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    
    /**
     * 뉴스 ID로 뉴스 상세 정보 조회
     */
    Optional<News> findByNewsId(Long newsId);
    
    /**
     * 카테고리별 뉴스 목록 조회 (페이징)
     */
    Page<News> findByCategoryNameOrderByCreatedAtDescNewsIdDesc(News.CategoryType categoryName, Pageable pageable);
    
    /**
     * 모든 뉴스 목록 조회 (페이징) - 카테고리별로 골고루 섞어서 조회
     */
    @Query("SELECT n FROM News n ORDER BY n.categoryName, n.newsId DESC")
    Page<News> findAllByOrderByNewsIdDesc(Pageable pageable);
    
    /**
     * 제목으로 뉴스 검색 (페이징)
     */
    @Query("SELECT n FROM News n WHERE n.title LIKE %:keyword% ORDER BY n.createdAt DESC, n.newsId DESC")
    Page<News> findByTitleContaining(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 언론사별 뉴스 조회 (페이징)
     */
    Page<News> findByPressOrderByCreatedAtDescNewsIdDesc(String press, Pageable pageable);
    
    /**
     * 발행일 기간으로 뉴스 조회
     */
    @Query("SELECT n FROM News n WHERE n.publishedAt BETWEEN :startDate AND :endDate ORDER BY n.publishedAt DESC")
    List<News> findByPublishedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                       @Param("endDate") LocalDateTime endDate);
    
    /**
     * 중복 제거 상태별 뉴스 조회
     */
    List<News> findByDedupState(News.DedupState dedupState);
    
    /**
     * 최신 뉴스 Top N 조회
     */
    @Query("SELECT n FROM News n ORDER BY n.createdAt DESC, n.newsId DESC")
    List<News> findLatestNews(Pageable pageable);
}
