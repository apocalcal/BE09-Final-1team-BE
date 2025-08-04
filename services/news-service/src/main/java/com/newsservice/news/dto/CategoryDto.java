package com.newsservice.news.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CategoryDto {
    private Integer categoryId;
    private String categoryName;
    private String displayName;
    private String description;
    
    // 기본 생성자
    public CategoryDto() {}
    
    // 전체 생성자
    public CategoryDto(Integer categoryId, String categoryName, String displayName, String description) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.displayName = displayName;
        this.description = description;
    }
    
    // Builder 패턴
    public static CategoryDtoBuilder builder() {
        return new CategoryDtoBuilder();
    }
    
    // Getter/Setter 메서드들
    public Integer getCategoryId() { return categoryId; }
    public void setCategoryId(Integer categoryId) { this.categoryId = categoryId; }
    
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    // Builder 클래스
    public static class CategoryDtoBuilder {
        private Integer categoryId;
        private String categoryName;
        private String displayName;
        private String description;
        
        public CategoryDtoBuilder categoryId(Integer categoryId) {
            this.categoryId = categoryId;
            return this;
        }
        
        public CategoryDtoBuilder categoryName(String categoryName) {
            this.categoryName = categoryName;
            return this;
        }
        
        public CategoryDtoBuilder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }
        
        public CategoryDtoBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public CategoryDto build() {
            return new CategoryDto(categoryId, categoryName, displayName, description);
        }
    }
} 