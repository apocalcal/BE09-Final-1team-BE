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
    @Query("SELECT n FROM News n WHERE n.categoryName = :category")
    Page<News> findByCategory(@Param("category") News.Category category, Pageable pageable);
    
    // 키워드 검색 (제목, 내용, 요약에서 검색)
    @Query("SELECT n FROM News n WHERE " +
           "n.title LIKE %:keyword% OR n.content LIKE %:keyword% OR n.summary LIKE %:keyword%")
    Page<News> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    // 최신 뉴스 조회 (발행일 기준 내림차순)
    @Query("SELECT n FROM News n ORDER BY n.createdAt DESC")
    Page<News> findLatestNews(Pageable pageable);
    
    // 인기 뉴스 조회 (신뢰도 기준 내림차순)
    @Query("SELECT n FROM News n ORDER BY n.trusted DESC")
    Page<News> findPopularNews(Pageable pageable);
    
    // 트렌딩 뉴스 조회 (신뢰도 + 생성일 기준)
    @Query("SELECT n FROM News n ORDER BY n.trusted DESC, n.createdAt DESC")
    Page<News> findTrendingNews(Pageable pageable);
    
    // 특정 기간 내 뉴스 조회 (페이징)
    @Query("SELECT n FROM News n WHERE n.createdAt BETWEEN :startDate AND :endDate")
    Page<News> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate, 
                                     Pageable pageable);
    
    // 특정 기간 내 뉴스 조회 (List 반환)
    @Query("SELECT n FROM News n WHERE n.createdAt BETWEEN :startDate AND :endDate ORDER BY n.createdAt DESC")
    List<News> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate, 
                                     @Param("endDate") LocalDateTime endDate);
    
    // 신뢰도가 높은 뉴스 조회
    @Query("SELECT n FROM News n WHERE n.trusted = true")
    Page<News> findByTrustedTrue(Pageable pageable);
    
    // 원본 뉴스 ID로 조회
    List<News> findByOriginalNewsId(Long originalNewsId);
    
    // 요약이 있는 뉴스만 조회
    @Query("SELECT n FROM News n WHERE n.summary IS NOT NULL AND n.summary != ''")
    Page<News> findWithSummary(Pageable pageable);
    
    // 특정 언론사 뉴스 조회
    @Query("SELECT n FROM News n WHERE n.press = :press")
    Page<News> findByPress(@Param("press") String press, Pageable pageable);
    
    // 카테고리별 뉴스 개수 조회
    @Query("SELECT COUNT(n) FROM News n WHERE n.categoryName = :category")
    Long countByCategory(@Param("category") News.Category category);
    
    // 전체 뉴스 조회 (페이징)
    Page<News> findAll(Pageable pageable);
    
    // 연관뉴스 조회를 위한 메서드들
    
    // oid_aid 리스트로 뉴스 조회
    @Query("SELECT n FROM News n WHERE n.oidAid IN :oidAids")
    List<News> findByOidAidIn(@Param("oidAids") List<String> oidAids);
    
    // 같은 생성일, 같은 카테고리, 특정 뉴스 제외
    @Query("SELECT n FROM News n WHERE n.createdAt = :createdAt AND n.categoryName = :categoryName AND n.newsId != :excludeNewsId")
    List<News> findByCreatedAtAndCategoryNameAndNewsIdNot(@Param("createdAt") LocalDateTime createdAt, 
                                                          @Param("categoryName") News.Category categoryName, 
                                                          @Param("excludeNewsId") Long excludeNewsId);
    
    // oid_aid 리스트로 뉴스 조회 (특정 뉴스 제외)
    @Query("SELECT n FROM News n WHERE n.oidAid IN :oidAids AND n.newsId != :excludeNewsId")
    List<News> findByOidAidInAndNewsIdNot(@Param("oidAids") List<String> oidAids, 
                                          @Param("excludeNewsId") Long excludeNewsId);
    
    // 특정 기간, 같은 카테고리, 특정 뉴스들 제외
    @Query("SELECT n FROM News n WHERE n.createdAt BETWEEN :startDate AND :endDate AND n.categoryName = :categoryName AND n.newsId NOT IN :excludeNewsIds")
    List<News> findByCreatedAtBetweenAndCategoryNameAndNewsIdNotIn(@Param("startDate") LocalDateTime startDate, 
                                                                   @Param("endDate") LocalDateTime endDate, 
                                                                   @Param("categoryName") News.Category categoryName, 
                                                                   @Param("excludeNewsIds") List<Long> excludeNewsIds);
} 