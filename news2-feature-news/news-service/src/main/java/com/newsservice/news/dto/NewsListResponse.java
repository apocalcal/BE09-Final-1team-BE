package com.newsservice.news.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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
    
    // 기본 생성자
    public NewsListResponse() {}
    
    // 전체 생성자
    public NewsListResponse(Long newsId, Long originalNewsId, String title, String summary,
                           String press, String link, Integer trusted, LocalDateTime publishedAt,
                           LocalDateTime createdAt, String reporterName, Integer viewCount) {
        this.newsId = newsId;
        this.originalNewsId = originalNewsId;
        this.title = title;
        this.summary = summary;
        this.press = press;
        this.link = link;
        this.trusted = trusted;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.reporterName = reporterName;
        this.viewCount = viewCount;
    }
    
    // Builder 패턴
    public static NewsListResponseBuilder builder() {
        return new NewsListResponseBuilder();
    }
    
    // Getter/Setter 메서드들
    public Long getNewsId() { return newsId; }
    public void setNewsId(Long newsId) { this.newsId = newsId; }
    
    public Long getOriginalNewsId() { return originalNewsId; }
    public void setOriginalNewsId(Long originalNewsId) { this.originalNewsId = originalNewsId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public String getPress() { return press; }
    public void setPress(String press) { this.press = press; }
    
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    
    public Integer getTrusted() { return trusted; }
    public void setTrusted(Integer trusted) { this.trusted = trusted; }
    
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    
    // Builder 클래스
    public static class NewsListResponseBuilder {
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
        
        public NewsListResponseBuilder newsId(Long newsId) {
            this.newsId = newsId;
            return this;
        }
        
        public NewsListResponseBuilder originalNewsId(Long originalNewsId) {
            this.originalNewsId = originalNewsId;
            return this;
        }
        
        public NewsListResponseBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        public NewsListResponseBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }
        
        public NewsListResponseBuilder press(String press) {
            this.press = press;
            return this;
        }
        
        public NewsListResponseBuilder link(String link) {
            this.link = link;
            return this;
        }
        
        public NewsListResponseBuilder trusted(Integer trusted) {
            this.trusted = trusted;
            return this;
        }
        
        public NewsListResponseBuilder publishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }
        
        public NewsListResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public NewsListResponseBuilder reporterName(String reporterName) {
            this.reporterName = reporterName;
            return this;
        }
        
        public NewsListResponseBuilder viewCount(Integer viewCount) {
            this.viewCount = viewCount;
            return this;
        }
        
        public NewsListResponse build() {
            return new NewsListResponse(newsId, originalNewsId, title, summary, press, link,
                                     trusted, publishedAt, createdAt, reporterName, viewCount);
        }
    }
} 