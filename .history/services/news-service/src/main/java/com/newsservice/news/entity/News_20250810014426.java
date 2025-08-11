package com.newsservice.news.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "news")
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "original_news_id", nullable = false)
    private Long originalNewsId;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private Integer trusted;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // NewsCrawl과의 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_news_id", insertable = false, updatable = false)
    private NewsCrawl originalNews;

    // Category enum 사용
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category;

    // 뉴스레터와의 N:N 연결
    @OneToMany(mappedBy = "news")
    private List<NewsletterNews> newsletterNewsList;
    
    // 기본 생성자
    public News() {}
    
    // 전체 생성자
    public News(Long id, Long originalNewsId, LocalDateTime publishedAt, String summary,
               Integer trusted, LocalDateTime createdAt, LocalDateTime updatedAt,
               NewsCrawl originalNews, Category category, List<NewsletterNews> newsletterNewsList) {
        this.id = id;
        this.originalNewsId = originalNewsId;
        this.publishedAt = publishedAt;
        this.summary = summary;
        this.trusted = trusted;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.originalNews = originalNews;
        this.category = category;
        this.newsletterNewsList = newsletterNewsList;
    }
    
    // Builder 패턴
    public static NewsBuilder builder() {
        return new NewsBuilder();
    }
    
    // Getter/Setter 메서드들
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getOriginalNewsId() { return originalNewsId; }
    public void setOriginalNewsId(Long originalNewsId) { this.originalNewsId = originalNewsId; }
    
    public LocalDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(LocalDateTime publishedAt) { this.publishedAt = publishedAt; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public Integer getTrusted() { return trusted; }
    public void setTrusted(Integer trusted) { this.trusted = trusted; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    
    public NewsCrawl getOriginalNews() { return originalNews; }
    public void setOriginalNews(NewsCrawl originalNews) { this.originalNews = originalNews; }
    
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    public List<NewsletterNews> getNewsletterNewsList() { return newsletterNewsList; }
    public void setNewsletterNewsList(List<NewsletterNews> newsletterNewsList) { 
        this.newsletterNewsList = newsletterNewsList; 
    }
    
    // Builder 클래스
    public static class NewsBuilder {
        private Long id;
        private Long originalNewsId;
        private LocalDateTime publishedAt;
        private String summary;
        private Integer trusted;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private NewsCrawl originalNews;
        private Category category;
        private List<NewsletterNews> newsletterNewsList;
        
        public NewsBuilder id(Long id) {
            this.id = id;
            return this;
        }
        
        public NewsBuilder originalNewsId(Long originalNewsId) {
            this.originalNewsId = originalNewsId;
            return this;
        }
        
        public NewsBuilder publishedAt(LocalDateTime publishedAt) {
            this.publishedAt = publishedAt;
            return this;
        }
        
        public NewsBuilder summary(String summary) {
            this.summary = summary;
            return this;
        }
        
        public NewsBuilder trusted(Integer trusted) {
            this.trusted = trusted;
            return this;
        }
        
        public NewsBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public NewsBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public NewsBuilder originalNews(NewsCrawl originalNews) {
            this.originalNews = originalNews;
            return this;
        }
        
        public NewsBuilder category(Category category) {
            this.category = category;
            return this;
        }
        
        public NewsBuilder newsletterNewsList(List<NewsletterNews> newsletterNewsList) {
            this.newsletterNewsList = newsletterNewsList;
            return this;
        }
        
        public News build() {
            return new News(id, originalNewsId, publishedAt, summary, trusted, 
                          createdAt, updatedAt, originalNews, category, newsletterNewsList);
        }
    }
}