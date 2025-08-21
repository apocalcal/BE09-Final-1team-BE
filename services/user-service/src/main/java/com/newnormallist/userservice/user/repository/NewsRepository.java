package com.newnormallist.userservice.user.repository;

import com.newnormallist.userservice.user.entity.News;
import com.newnormallist.userservice.user.entity.NewsCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NewsRepository extends JpaRepository<News, Long> {
    
    @Query("SELECT n.categoryName FROM News n WHERE n.newsId = :newsId")
    Optional<NewsCategory> findCategoryById(@Param("newsId") Long newsId);
}
