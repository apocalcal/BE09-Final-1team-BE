package com.newnormallist.newsservice.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

import com.newnormallist.newsservice.recommendation.entity.ScrapStorages;

public interface ScrapStorageRepository extends JpaRepository<ScrapStorages, Integer> {
    List<ScrapStorages> findByUserId(Long userId);
}