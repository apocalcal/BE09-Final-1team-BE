package com.newsservice.news.repository;

import com.newsservice.news.entity.NewsComplaint;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsComplaintRepository extends JpaRepository<NewsComplaint, Long> {
    long countByNewsId(Long newsId);
}
