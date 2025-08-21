package com.newnormallist.newsservice.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import java.util.List;
import java.util.Collection;

import com.newnormallist.newsservice.recommendation.entity.News;
import com.newnormallist.newsservice.recommendation.entity.Category;


// findLatestIdsByCategory(cat, limit) : 카테고리 최신 뉴스 ID 가져오기.
// findByIdIn(ids) : 메타 정보 일괄 조회.
// findCategoryById(id) : 조회 로그 저장 시 newsId → category 팝업용.
public interface NewsRepository extends JpaRepository<News, Long> {

    @Query("select n.id from News n where n.category = :cat order by n.publishedAt desc")
    List<Long> findLatestIdsByCategory(@Param("cat") Category category, Pageable pageable);

    List<News> findByIdIn(Collection<Long> ids);

    @Query("select n.category from News n where n.id = :id")
    Category findCategoryById(@Param("id") Long id);
    
    // published_at 기준 최신순 정렬 (전체 뉴스 피드용)
    @Query("select n from News n order by n.publishedAt desc")
    Page<News> findAllByOrderByPublishedAtDesc(Pageable pageable);
}
