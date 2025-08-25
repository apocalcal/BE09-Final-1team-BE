package com.newnormallist.newsservice.recommendation.mapper;

import com.newnormallist.newsservice.recommendation.dto.FeedItemDto;
import com.newnormallist.newsservice.recommendation.entity.NewsEntity;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

// News 엔티티를 FeedItemDto로 변환하는 매퍼
public class FeedMapper {
    public static FeedItemDto toDto(NewsEntity newsEntity) {
        // publishedAt을 LocalDateTime으로 변환
        LocalDateTime publishedAt = null;
        if (newsEntity.getPublishedAt() != null && !newsEntity.getPublishedAt().isEmpty()) {
            try {
                if (newsEntity.getPublishedAt().contains("T")) {
                    // ISO 8601 형식: 2025-08-20T09:35:11
                    publishedAt = LocalDateTime.parse(newsEntity.getPublishedAt(), 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
                } else {
                    // 일반 형식: 2025-08-20 09:35:11
                    publishedAt = LocalDateTime.parse(newsEntity.getPublishedAt(), 
                        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                }
            } catch (Exception e) {
                // 파싱 실패 시 null로 설정
                publishedAt = null;
            }
        }

        return FeedItemDto.builder()
            .newsId(newsEntity.getNewsId())
            .title(newsEntity.getTitle())
            .press(newsEntity.getPress())
            .link(newsEntity.getLink())
            .trusted(newsEntity.getTrusted())
            .publishedAt(publishedAt)
            .createdAt(newsEntity.getCreatedAt())
            .reporter(newsEntity.getReporter())
            .categoryName(newsEntity.getCategoryName())
            .dedupState(newsEntity.getDedupState())
            .imageUrl(newsEntity.getImageUrl())
            .oidAid(newsEntity.getOidAid())
            .updatedAt(newsEntity.getUpdatedAt())
            .build();
    }
}
