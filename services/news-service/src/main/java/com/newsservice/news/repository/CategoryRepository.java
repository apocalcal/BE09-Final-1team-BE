package com.newsservice.news.repository;

import com.newsservice.news.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    
    // 카테고리 이름으로 검색
    Category findByName(String name);
}
