package com.newnormallist.newsservice.recommendation.mapper;

import com.newnormallist.newsservice.recommendation.dto.FeedItemDto;
import com.newnormallist.newsservice.recommendation.entity.NewsEntity;

import java.time.LocalDateTime;

// News 엔티티를 FeedItemDto로 변환하는 매퍼
public class FeedMapper {
    public static FeedItemDto toDto(NewsEntity newsEntity) {
        return FeedItemDto.builder()
            .newsId(newsEntity.getNewsId())
            .title(newsEntity.getTitle())
            .press(newsEntity.getPress())
            .link(newsEntity.getLink())
            .trusted(newsEntity.getTrusted())
            .publishedAt(newsEntity.getPublishedAt())
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
