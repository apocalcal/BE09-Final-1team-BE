package com.newsservice.news.repository;

import com.newsservice.news.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Integer> {
    
    // 카테고리 이름으로 검색
    Category findByName(String name);
    
    // 카테고리 이름으로 검색 (Optional 반환)
    Optional<Category> findByNameOptional(String name);
    
    // 활성화된 카테고리만 조회
    @Query("SELECT c FROM Category c WHERE c.active = true")
    List<Category> findActiveCategories();
    
    // 카테고리 이름에 특정 키워드가 포함된 카테고리 조회
    @Query("SELECT c FROM Category c WHERE c.name LIKE %:keyword%")
    List<Category> findByNameContaining(@Param("keyword") String keyword);
    
    // 카테고리 설명에 특정 키워드가 포함된 카테고리 조회
    @Query("SELECT c FROM Category c WHERE c.description LIKE %:keyword%")
    List<Category> findByDescriptionContaining(@Param("keyword") String keyword);
    
    // 특정 ID 범위의 카테고리 조회
    @Query("SELECT c FROM Category c WHERE c.id BETWEEN :startId AND :endId")
    List<Category> findByIdBetween(@Param("startId") Integer startId, @Param("endId") Integer endId);
    
    // 카테고리 이름으로 존재 여부 확인
    boolean existsByName(String name);
    
    // 카테고리 ID로 존재 여부 확인
    boolean existsById(Integer id);
    
    // 카테고리 개수 조회
    @Query("SELECT COUNT(c) FROM Category c")
    long countCategories();
    
    // 활성화된 카테고리 개수 조회
    @Query("SELECT COUNT(c) FROM Category c WHERE c.active = true")
    long countActiveCategories();
} 