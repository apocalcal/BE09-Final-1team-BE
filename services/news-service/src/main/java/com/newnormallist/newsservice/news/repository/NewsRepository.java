package com.newnormallist.newsservice.news.repository;

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

    @Query("SELECT n FROM News n WHERE STR_TO_DATE(n.publishedAt, '%Y-%m-%d %H:%i:%s') > :since")
    List<News> findByPublishedAtAfter(@Param("since") LocalDateTime since);

    // 카테고리별 뉴스 조회 (최신순)
    @Query("SELECT n FROM News n WHERE n.categoryName = :category ORDER BY STR_TO_DATE(n.publishedAt, '%Y-%m-%d %H:%i:%s') DESC")
    Page<News> findByCategory(@Param("category") Category category, Pageable pageable);

    @Query("SELECT n FROM News n WHERE (n.title LIKE %:keyword% OR n.content LIKE %:keyword%) AND n.status = 'PUBLISHED' ORDER BY n.createdAt DESC")
    Page<News> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.status = 'PUBLISHED' ORDER BY n.createdAt DESC")
    Page<News> findLatestNews(Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.status = 'PUBLISHED' ORDER BY n.trusted DESC")
    Page<News> findPopularNews(Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.status = 'PUBLISHED' ORDER BY n.trusted DESC, n.createdAt DESC")
    Page<News> findTrendingNews(Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.createdAt BETWEEN :startDate AND :endDate AND n.status = 'PUBLISHED'")
    Page<News> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.createdAt BETWEEN :startDate AND :endDate AND n.status = 'PUBLISHED' ORDER BY n.createdAt DESC")
    List<News> findByCreatedAtBetween(@Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT n FROM News n WHERE n.trusted = true AND n.status = 'PUBLISHED' ORDER BY n.createdAt DESC")
    Page<News> findByTrustedTrue(Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.press = :press AND n.status = 'PUBLISHED' ORDER BY n.createdAt DESC")
    Page<News> findByPress(@Param("press") String press, Pageable pageable);

    @Query("SELECT COUNT(n) FROM News n WHERE n.categoryName = :category AND n.status = 'PUBLISHED'")
    Long countByCategory(@Param("category") Category category);

    @Query("SELECT n FROM News n WHERE n.status = 'PUBLISHED'")
    Page<News> findAll(Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.status = 'PUBLISHED' ORDER BY n.createdAt DESC")
    Page<News> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("SELECT n FROM News n WHERE n.status = 'PUBLISHED' ORDER BY n.publishedAt DESC")
    Page<News> findAllByOrderByPublishedAtDesc(Pageable pageable);

    // 연관뉴스 조회를 위한 메서드들

    @Query("SELECT n FROM News n WHERE n.oidAid IN :oidAids AND n.status = 'PUBLISHED'")
    List<News> findByOidAidIn(@Param("oidAids") List<String> oidAids);

    // 이 메서드의 날짜 타입을 String으로 다시 변경
    @Query("SELECT n FROM News n WHERE n.publishedAt = :publishedAt AND n.categoryName = :categoryName AND n.newsId != :excludeNewsId AND n.status = 'PUBLISHED'")
    List<News> findByPublishedAtAndCategoryNameAndNewsIdNot(@Param("publishedAt") String publishedAt,
            @Param("categoryName") Category categoryName,
            @Param("excludeNewsId") Long excludeNewsId);

    @Query("SELECT n FROM News n WHERE n.oidAid IN :oidAids AND n.newsId != :excludeNewsId AND n.status = 'PUBLISHED'")
    List<News> findByOidAidInAndNewsIdNot(@Param("oidAids") List<String> oidAids,
            @Param("excludeNewsId") Long excludeNewsId);

    // 이 메서드의 날짜 타입을 String으로 다시 변경
    @Query("SELECT n FROM News n WHERE n.publishedAt BETWEEN :startDate AND :endDate AND n.categoryName = :categoryName AND n.newsId NOT IN :excludeNewsIds AND n.status = 'PUBLISHED'")
    List<News> findByPublishedAtBetweenAndCategoryNameAndNewsIdNotIn(@Param("startDate") String startDate,
            @Param("endDate") String endDate,
            @Param("categoryName") Category categoryName,
            @Param("excludeNewsIds") List<Long> excludeNewsIds);
}