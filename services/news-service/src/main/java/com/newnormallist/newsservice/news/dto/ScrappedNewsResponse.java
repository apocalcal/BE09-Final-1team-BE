package com.newnormallist.newsservice.news.dto;

import com.newnormallist.newsservice.news.entity.News;
import com.newnormallist.newsservice.news.entity.NewsScrap;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ScrappedNewsResponse {
    private Long newsId;
    private String title;
    private String press;
    private String imageUrl;
    private LocalDateTime scrappedAt;

    public static ScrappedNewsResponse from(NewsScrap newsScrap) {
        News news = newsScrap.getNews();
        return ScrappedNewsResponse.builder()
                .newsId(news.getNewsId())
                .title(news.getTitle())
                .press(news.getPress())
                .imageUrl(news.getImageUrl())
                .scrappedAt(newsScrap.getCreatedAt()) // 스크랩된 시각
                .build();
    }
}
