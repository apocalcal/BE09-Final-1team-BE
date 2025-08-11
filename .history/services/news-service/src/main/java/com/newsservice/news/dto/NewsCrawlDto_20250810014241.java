package com.newsservice.news.dto;

import com.newsservice.news.entity.Category;
import java.time.LocalDateTime;

public class NewsCrawlDto {
    
    private Long linkId;
    private String title;
    private String press;
    private String content;
    private String reporterName;
    private LocalDateTime publishedAt;
    private Category category;
    
    // 기본 생성자
    public NewsCrawlDto() {}
    
    // 전체 생성자
    public NewsCrawlDto(Long linkId, String title, String press, String content, 
                       String reporterName, LocalDateTime publishedAt, Category category) {
        this.linkId = linkId;
        this.title = title;
        this.press = press;
        this.content = content;
        this.reporterName = reporterName;
        this.publishedAt = publishedAt;
        this.category = category;
    }
    
    // Builder 패턴
    public static NewsCrawlDtoBuilder builder() {
        return new NewsCrawlDtoBuilder();
    }
    
    // Getter/Setter 메서드들
    public Long getLinkId() { return linkId; }
    public void setLinkId(Long linkId) { this.linkId = linkId; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getPress() { return press; }
    public void setPress(String press) { this.press = press; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }
    
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    // Builder 클래스
    public static class NewsCrawlDtoBuilder {
        private Long linkId;
        private String title;
        private String press;
        private String content;
        private String reporterName;
        private LocalDateTime publishedAt;
        private Integer categoryId;
        
        public NewsCrawlDtoBuilder linkId(Long linkId) {
            this.linkId = linkId;
            return this;
        }
        
        public NewsCrawlDtoBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        public NewsCrawlDtoBuilder press(String press) {
            this.press = press;
            return this;
        }
        
        public NewsCrawlDtoBuilder content(String content) {
            this.content = content;
            return this;
        }
        
        public NewsCrawlDtoBuilder reporterName(String reporterName) {
            this.reporterName = reporterName;
            return this;
        }
        
        public NewsCrawlDtoBuilder publishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }
        
        public NewsCrawlDtoBuilder categoryId(Integer categoryId) {
            this.categoryId = categoryId;
            return this;
        }
        
        public NewsCrawlDto build() {
            return new NewsCrawlDto(linkId, title, press, content, reporterName, publishedAt, categoryId);
        }
    }
}