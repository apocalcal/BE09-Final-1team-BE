package com.newnormallist.newsservice.news.dto;

import com.newnormallist.newsservice.news.entity.News;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsListResponse {
    private Long newsId;
    private String title;
    private String content;
    private String summary;
    private String press;
    private String link;
    private Integer trusted;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private String reporterName;
    private Integer viewCount;
    private String categoryName;
    private String categoryDescription;
    private String dedupState;
    private String dedupStateDescription;
    private String imageUrl;
    private String oidAid;

    public static NewsListResponse from(News news) {
        if (news == null) {
            // 스크랩은 존재하지만 원본 뉴스가 삭제된 경우, 플레이스홀더를 반환합니다.
            return NewsListResponse.builder()
                    .newsId(0L)
                    .title("[삭제된 뉴스]")
                    .press("-")
                    .categoryName("기타")
                    .build();
        }
        return NewsListResponse.builder()
                .newsId(news.getNewsId())
                .title(news.getTitle())
                .press(news.getPress())
                .reporterName(news.getReporter())
                .categoryName(news.getCategoryName() != null ? news.getCategoryName().getCategoryName() : null)
                .imageUrl(news.getImageUrl())
                .createdAt(news.getCreatedAt())
                .updatedAt(news.getUpdatedAt())
                .build();
    }
}
