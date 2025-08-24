package com.newsservice.news.repository;

import com.newsservice.news.entity.ScrapStorage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ScrapStorageRepository extends JpaRepository<ScrapStorage, Integer> {
    Optional<ScrapStorage> findByUserId(Long userId);

    // ID 생성을 위해 가장 큰 storage_id를 찾는 메소드 추가
    Optional<ScrapStorage> findTopByOrderByStorageIdDesc();
}
