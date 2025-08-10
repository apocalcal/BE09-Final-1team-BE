package com.newnormallist.newsservice.dto;

import com.newnormallist.newsservice.entity.News;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsListResponseDto {
    
    private Long newsId;
    private String categoryName;
    private String categoryDescription;
    private String title;
    private String content;
    private String press;
    private LocalDateTime publishedAt;
    private String reporter;
    private String dedupState;
    private String dedupStateDescription;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Entity에서 DTO로 변환하는 정적 메서드
    public static NewsListResponseDto from(News news) {
        return NewsListResponseDto.builder()
                .newsId(news.getNewsId())
                .categoryName(news.getCategoryName().name())
                .categoryDescription(news.getCategoryName().getDescription())
                .title(news.getTitle())
                .content(news.getContent())
                .press(news.getPress())
                .publishedAt(news.getPublishedAt())
                .reporter(news.getReporter())
                .dedupState(news.getDedupState().name())
                .dedupStateDescription(news.getDedupState().getDescription())
                .createdAt(news.getCreatedAt())
                .updatedAt(news.getUpdatedAt())
                .build();
    }
}
