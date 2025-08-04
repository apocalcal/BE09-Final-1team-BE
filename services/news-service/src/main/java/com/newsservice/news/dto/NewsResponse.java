package com.newsservice.news.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class NewsResponse {
    private Long newsId;
    private Long originalNewsId;
    private String title;
    private String content;
    private String press;
    private String link;
    private String summary;
    private Integer trusted;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private String reporterName;
    
    // 기본 생성자
    public NewsResponse() {}
    
    // 전체 생성자
    public NewsResponse(Long newsId, Long originalNewsId, String title, String content,
                       String press, String link, String summary, Integer trusted,
                       LocalDateTime publishedAt, LocalDateTime createdAt, String reporterName) {
        this.newsId = newsId;
        this.originalNewsId = originalNewsId;
        this.title = title;
        this.content = content;
        this.press = press;
        this.link = link;
        this.summary = summary;
        this.trusted = trusted;
        this.publishedAt = publishedAt;
        this.createdAt = createdAt;
        this.reporterName = reporterName;
    }
    
    // Builder 패턴
    public static NewsResponseBuilder builder() {
        return new NewsResponseBuilder();
    }
    
    // Getter/Setter 메서드들
    public Long getNewsId() { return newsId; }
    public void setNewsId(Long newsId) { this.newsId = newsId; }
    
    public Long getOriginalNewsId() { return originalNewsId; }
    public void setOriginalNewsId(Long originalNewsId) { this.originalNewsId = originalNewsId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getPress() { return press; }
    public void setPress(String press) { this.press = press; }
    
    public String getLink() { return link; }
    public void setLink(String link) { this.link = link; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public Integer getTrusted() { return trusted; }
    public void setTrusted(Integer trusted) { this.trusted = trusted; }
    
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    
    // Builder 클래스
    public static class NewsResponseBuilder {
        private Long newsId;
        private Long originalNewsId;
        private String title;
        private String content;
        private String press;
        private String link;
        private String summary;
        private Integer trusted;
        private LocalDateTime publishedAt;
        private LocalDateTime createdAt;
        private String reporterName;
        
        public NewsResponseBuilder newsId(Long newsId) {
            this.newsId = newsId;
            return this;
        }
        
        public NewsResponseBuilder originalNewsId(Long originalNewsId) {
            this.originalNewsId = originalNewsId;
            return this;
        }
        
        public NewsResponseBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        public NewsResponseBuilder content(String content) {
            this.content = content;
            return this;
        }
        
        public NewsResponseBuilder press(String press) {
            this.press = press;
            return this;
        }
        
        public NewsResponseBuilder link(String link) {
            this.link = link;
            return this;
        }
        
        public NewsResponseBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }
        
        public NewsResponseBuilder trusted(Integer trusted) {
            this.trusted = trusted;
            return this;
        }
        
        public NewsResponseBuilder publishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }
        
        public NewsResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public NewsResponseBuilder reporterName(String reporterName) {
            this.reporterName = reporterName;
            return this;
        }
        
        public NewsResponse build() {
            return new NewsResponse(newsId, originalNewsId, title, content, press, link,
                                 summary, trusted, publishedAt, createdAt, reporterName);
        }
    }
} 