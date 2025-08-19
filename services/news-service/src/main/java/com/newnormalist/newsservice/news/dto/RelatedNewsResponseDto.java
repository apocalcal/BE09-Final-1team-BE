package com.newnormalist.newsservice.news.dto;

import com.newnormalist.newsservice.news.entity.Category;
import com.newnormalist.newsservice.news.entity.News;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelatedNewsResponseDto {

    private Long newsId;
    private String title;
    private String press;
    private String publishedAt;
    private String reporter;
    private LocalDateTime createdAt;
    private String imageUrl;
    private String summary;
    private Category categoryName;

    public static RelatedNewsResponseDto from(News news) {
        return RelatedNewsResponseDto.builder()
                .newsId(news.getNewsId())
                .title(news.getTitle())
                .press(news.getPress())
                .publishedAt(news.getPublishedAt())
                .reporter(news.getReporter())
                .createdAt(news.getCreatedAt())
                .imageUrl(news.getImageUrl())
                .categoryName(news.getCategoryName())
                .build();
    }
}
