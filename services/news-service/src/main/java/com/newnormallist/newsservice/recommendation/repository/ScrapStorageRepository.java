package com.newnormallist.newsservice.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import com.newnormallist.newsservice.recommendation.entity.ScrapStorage;

public interface ScrapStorageRepository extends JpaRepository<ScrapStorage, Integer> {
    List<ScrapStorage> findByUserId(Long userId);
}