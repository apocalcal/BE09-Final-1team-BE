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
    
    // 고급 키워드 검색 (정렬 옵션 포함)
    @Query("SELECT n FROM News n WHERE " +
           "(:query IS NULL OR (n.title LIKE %:query% OR n.content LIKE %:query% OR n.summary LIKE %:query%)) AND " +
           "(:category IS NULL OR n.categoryName = :category) AND " +
           "(:press IS NULL OR n.press = :press)")
    Page<News> searchByKeywordAdvanced(@Param("query") String query, 
                                      @Param("category") News.Category category, 
                                      @Param("press") String press, 
                                      Pageable pageable);
    
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
    
    // === 새로운 고급 검색 메서드들 ===
    
    // 자동완성을 위한 제목 키워드 추출
    @Query("SELECT DISTINCT n.title FROM News n WHERE n.title LIKE %:keyword% ORDER BY n.createdAt DESC")
    List<String> findTitlesByKeyword(@Param("keyword") String keyword, Pageable pageable);
    
    // 검색 통계를 위한 언론사별 개수
    @Query("SELECT n.press, COUNT(n) FROM News n WHERE " +
           "n.title LIKE %:query% OR n.content LIKE %:query% OR n.summary LIKE %:query% " +
           "GROUP BY n.press")
    List<Object[]> getPressStatsByQuery(@Param("query") String query);
    
    // 검색 통계를 위한 카테고리별 개수
    @Query("SELECT n.categoryName, COUNT(n) FROM News n WHERE " +
           "n.title LIKE %:query% OR n.content LIKE %:query% OR n.summary LIKE %:query% " +
           "GROUP BY n.categoryName")
    List<Object[]> getCategoryStatsByQuery(@Param("query") String query);
    
    // 고급 검색 (여러 조건 조합)
    @Query("SELECT n FROM News n WHERE " +
           "(:query IS NULL OR (n.title LIKE %:query% OR n.content LIKE %:query% OR n.summary LIKE %:query%)) AND " +
           "(:category IS NULL OR n.categoryName = :category) AND " +
           "(:press IS NULL OR n.press = :press) AND " +
           "(:reporter IS NULL OR n.reporter = :reporter) AND " +
           "(:startDate IS NULL OR n.createdAt >= :startDate) AND " +
           "(:endDate IS NULL OR n.createdAt <= :endDate) AND " +
           "(:trusted IS NULL OR n.trusted = :trusted)")
    Page<News> advancedSearch(@Param("query") String query,
                             @Param("category") News.Category category,
                             @Param("press") String press,
                             @Param("reporter") String reporter,
                             @Param("startDate") LocalDateTime startDate,
                             @Param("endDate") LocalDateTime endDate,
                             @Param("trusted") Boolean trusted,
                             Pageable pageable);
    
    // 인기 키워드 추출 (제목에서 자주 나오는 단어들)
    @Query("SELECT DISTINCT SUBSTRING(n.title, 1, 10) FROM News n WHERE n.title IS NOT NULL ORDER BY n.createdAt DESC")
    List<String> findPopularKeywords(Pageable pageable);
    
    // 관련 키워드 추천 (같은 카테고리에서 자주 나오는 단어들)
    @Query("SELECT DISTINCT n.title FROM News n WHERE " +
           "n.categoryName = (SELECT n2.categoryName FROM News n2 WHERE n2.title LIKE %:query% LIMIT 1) AND " +
           "n.title NOT LIKE %:query% ORDER BY n.createdAt DESC")
    List<String> findRelatedKeywords(@Param("query") String query, Pageable pageable);
} 