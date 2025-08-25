package com.newnormallist.newsservice.recommendation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

import com.newnormallist.newsservice.recommendation.entity.ScrapStorages;

@Repository("recommendationScrapStorageRepository")
public interface ScrapStorageRepository extends JpaRepository<ScrapStorages, Integer> {
    List<ScrapStorages> findByUserId(Long userId);
}