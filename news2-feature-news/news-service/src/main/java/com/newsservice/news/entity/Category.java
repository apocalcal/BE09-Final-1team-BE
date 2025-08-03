package com.newsservice.news.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    // NewsCrawl과의 관계
    @OneToMany(mappedBy = "category")
    private List<NewsCrawl> newsCrawls;

    // News와의 관계 (필요시)
    @OneToMany(mappedBy = "category")
    private List<News> news;
    
    // 기본 생성자
    public Category() {}
    
    // 전체 생성자
    public Category(Integer id, String name, String displayName, String description, 
                   Boolean active, List<NewsCrawl> newsCrawls, List<News> news) {
        this.id = id;
        this.name = name;
        this.displayName = displayName;
        this.description = description;
        this.active = active;
        this.newsCrawls = newsCrawls;
        this.news = news;
    }
    
    // Builder 패턴
    public static CategoryBuilder builder() {
        return new CategoryBuilder();
    }
    
    // Getter/Setter 메서드들
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }
    
    public List<NewsCrawl> getNewsCrawls() { return newsCrawls; }
    public void setNewsCrawls(List<NewsCrawl> newsCrawls) { this.newsCrawls = newsCrawls; }
    
    public List<News> getNews() { return news; }
    public void setNews(List<News> news) { this.news = news; }
    
    // Builder 클래스
    public static class CategoryBuilder {
        private Integer id;
        private String name;
        private String displayName;
        private String description;
        private Boolean active = true;
        private List<NewsCrawl> newsCrawls;
        private List<News> news;
        
        public CategoryBuilder id(Integer id) {
            this.id = id;
            return this;
        }
        
        public CategoryBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        public CategoryBuilder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }
        
        public CategoryBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public CategoryBuilder active(Boolean active) {
            this.active = active;
            return this;
        }
        
        public CategoryBuilder newsCrawls(List<NewsCrawl> newsCrawls) {
            this.newsCrawls = newsCrawls;
            return this;
        }
        
        public CategoryBuilder news(List<News> news) {
            this.news = news;
            return this;
        }
        
        public Category build() {
            return new Category(id, name, displayName, description, active, newsCrawls, news);
        }
    }
} 