package com.newsservice.news.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsListResponse {
    private Long newsId;
    private Long originalNewsId;
    private String title;
    private String summary;
    private String press;
    private String link;
    private Integer trusted;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private String reporterName;
    private Integer viewCount;
    private String categoryName;
    private String categoryDescription;
    private String dedupState;
    private String dedupStateDescription;
    private String imageUrl;
    private String oidAid;
    private LocalDateTime updatedAt;
} 