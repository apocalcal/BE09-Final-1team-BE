package com.newnormallist.newsservice.recommendation.mapper;

import com.newnormallist.newsservice.recommendation.dto.FeedItemDto;
import com.newnormallist.newsservice.recommendation.entity.News;
import java.time.LocalDateTime;

// News 엔티티를 FeedItemDto로 변환하는 매퍼
public class FeedMapper {
    public static FeedItemDto toDto(News news) {
        return FeedItemDto.builder()
            .newsId(news.getNewsId())
            .title(news.getTitle())
            .press(news.getPress())
            .link(news.getLink())
            .trusted(news.getTrusted())
            .publishedAt(LocalDateTime.parse(news.getPublishedAt()))
            .createdAt(news.getCreatedAt())
            .reporter(news.getReporter())   
            .categoryName(news.getCategoryName())
            .dedupState(news.getDedupState())
            .imageUrl(news.getImageUrl())
            .oidAid(news.getOidAid())
            .updatedAt(news.getUpdatedAt())
            .build();
    }
}
