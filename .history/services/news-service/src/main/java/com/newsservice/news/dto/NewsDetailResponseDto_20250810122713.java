package com.newsservice.news.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsDetailResponseDto {
    private Long newsId;
    private String categoryName;
    private String title;
    private String content;
    private String press;
    private LocalDateTime publishedAt;
    private String reporter;
    private String dedupState;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer trusted;
    private String imageUrl;
    private String oidAid;
    private Long originalNewsId;
    private String summary;
}
