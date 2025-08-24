package com.newsletterservice.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class NewsletterContent {
    private String title;
    private boolean personalized;
    private Long userId;
    private Long newsletterId;
    private List<Section> sections;
    private LocalDateTime generatedAt;
    
    @Data
    @Builder
    public static class Section {
        private String heading;
        private String sectionType; // "PERSONALIZED", "TRENDING", "CATEGORY", "LATEST"
        private List<Article> articles;
        private String description;
    }
    
    @Data
    @Builder
    public static class Article {
        private Long id;
        private String title;
        private String summary;
        private String category;
        private String url;
        private LocalDateTime publishedAt;
        private Double personalizedScore; // 개인화 점수
        private String imageUrl;
        private Integer viewCount;
        private Integer shareCount;
    }
}
