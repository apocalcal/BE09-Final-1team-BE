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
public class NewsListResponseDto {
    private Long newsId;
    private String categoryName;
    private String title;
    private String press;
    private LocalDateTime publishedAt;
    private String reporter;
    private Integer trusted;
    private String imageUrl;
    private String summary;
}
