package com.newsservice.news.repository;

import com.newnormallist.newsservice.news.entity.Category;
import com.newnormallist.newsservice.news.entity.News;
import com.newnormallist.newsservice.news.entity.NewsStatus;
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

    List<News> findByStatus(NewsStatus status);

    // 카테고리별 뉴스 조회
    @Query("SELECT n FROM News n WHERE n.categoryName = :category AND n.status = 'PUBLISHED'")
    Page<News> findByCategory(@Param("category") Category category, Pageable pageable);

    // 키워드 검색 (제목, 내용에서 검색)
    @Query("SELECT n FROM News n WHERE (n.title LIKE %:keyword% OR n.content LIKE %:keyword%) AND n.status = 'PUBLISHED'")
    Page<News> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // 최신 뉴스 조회 (발행일 기준 내림차순)
    @Query("SELECT n FROM News n WHERE n.status = 'PUBLISHED' ORDER BY n.createdAt DESC")
    Page<News> findLatestNews(Pageable pageable);

    // 인기 뉴스 조회 (신뢰도 기준 내림차순)
    @Query("SELECT n FROM News n WHERE n.status = 'PUBLISHED' ORDER BY n.trusted DESC")
    Page<News> findPopularNews(Pageable pageable);

    // 트렌딩 뉴스 조회 (신뢰도 + 생성일 기준)
    @Query("SELECT n FROM News n WHERE n.status = 'PUBLISHED' ORDER BY n.trusted DESC, n.createdAt DESC")
    Page<News> findTrendingNews(Pageable pageable);

    // 특정 기간 내 뉴스 조회 (페이징)
    @Query("SELECT n FROM News n WHERE n.createdAt BETWEEN :startDate AND :endDate AND n.status = 'PUBLISHED'")
    Page<News> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate,
                                     Pageable pageable);

    // 특정 기간 내 뉴스 조회 (List 반환)
    @Query("SELECT n FROM News n WHERE n.createdAt BETWEEN :startDate AND :endDate AND n.status = 'PUBLISHED' ORDER BY n.createdAt DESC")
    List<News> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
                                     @Param("endDate") LocalDateTime endDate);

    // 신뢰도가 높은 뉴스 조회
    @Query("SELECT n FROM News n WHERE n.trusted = true AND n.status = 'PUBLISHED'")
    Page<News> findByTrustedTrue(Pageable pageable);

    // 특정 언론사 뉴스 조회
    @Query("SELECT n FROM News n WHERE n.press = :press AND n.status = 'PUBLISHED'")
    Page<News> findByPress(@Param("press") String press, Pageable pageable);

    // 카테고리별 뉴스 개수 조회
    @Query("SELECT COUNT(n) FROM News n WHERE n.categoryName = :category AND n.status = 'PUBLISHED'")
    Long countByCategory(@Param("category") Category category);

    // 전체 뉴스 조회 (페이징) - 숨김 기사를 제외하기 위해 @Query 추가
    @Query("SELECT n FROM News n WHERE n.status = 'PUBLISHED'")
    Page<News> findAll(Pageable pageable);

    // 연관뉴스 조회를 위한 메서드들 (마찬가지로 PUBLISHED 상태인 것만 조회)

    @Query("SELECT n FROM News n WHERE n.oidAid IN :oidAids AND n.status = 'PUBLISHED'")
    List<News> findByOidAidIn(@Param("oidAids") List<String> oidAids);

    @Query("SELECT n FROM News n WHERE n.createdAt = :createdAt AND n.categoryName = :categoryName AND n.newsId != :excludeNewsId AND n.status = 'PUBLISHED'")
    List<News> findByCreatedAtAndCategoryNameAndNewsIdNot(@Param("createdAt") LocalDateTime createdAt,
                                                          @Param("categoryName") Category categoryName,
                                                          @Param("excludeNewsId") Long excludeNewsId);

    @Query("SELECT n FROM News n WHERE n.oidAid IN :oidAids AND n.newsId != :excludeNewsId AND n.status = 'PUBLISHED'")
    List<News> findByOidAidInAndNewsIdNot(@Param("oidAids") List<String> oidAids,
                                          @Param("excludeNewsId") Long excludeNewsId);

    @Query("SELECT n FROM News n WHERE n.createdAt BETWEEN :startDate AND :endDate AND n.categoryName = :categoryName AND n.newsId NOT IN :excludeNewsIds AND n.status = 'PUBLISHED'")
    List<News> findByCreatedAtBetweenAndCategoryNameAndNewsIdNotIn(@Param("startDate") LocalDateTime startDate,
                                                                   @Param("endDate") LocalDateTime endDate,
                                                                   @Param("categoryName") Category categoryName,
                                                                   @Param("excludeNewsIds") List<Long> excludeNewsIds);
}