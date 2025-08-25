package com.newnormallist.newsservice.news.repository;

import com.newnormallist.newsservice.news.entity.Category;
import com.newnormallist.newsservice.news.entity.NewsScrap;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface NewsScrapRepository extends JpaRepository<NewsScrap, Integer> {

    @Query(value = "SELECT ns FROM NewsScrap ns LEFT JOIN FETCH ns.news n WHERE ns.storageId = :storageId",
           countQuery = "SELECT count(ns) FROM NewsScrap ns WHERE ns.storageId = :storageId")
    Page<NewsScrap> findByStorageIdWithNews(@Param("storageId") Integer storageId, Pageable pageable);

    @Query(value = "SELECT ns FROM NewsScrap ns LEFT JOIN FETCH ns.news n WHERE ns.storageId = :storageId AND n.categoryName = :category",
           countQuery = "SELECT count(ns) FROM NewsScrap ns JOIN ns.news n WHERE ns.storageId = :storageId AND n.categoryName = :category")
    Page<NewsScrap> findByStorageIdAndNewsCategoryNameWithNews(@Param("storageId") Integer storageId, @Param("category") Category category, Pageable pageable);

    Page<NewsScrap> findByStorageId(Integer storageId, Pageable pageable);

    Optional<NewsScrap> findTopByOrderByScrapIdDesc();

    Optional<NewsScrap> findByStorageIdAndNewsNewsId(Integer storageId, Long newsId);

    @Modifying
    @Transactional
    void deleteByStorageId(Integer storageId);
}
