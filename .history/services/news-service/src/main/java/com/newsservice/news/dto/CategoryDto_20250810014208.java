package com.newsservice.news.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CategoryDto {
    private String categoryCode;
    private String categoryName;
    private String icon;
    
    // 기본 생성자
    public CategoryDto() {}
    
    // 전체 생성자
    public CategoryDto(String categoryCode, String categoryName, String icon) {
        this.categoryCode = categoryCode;
        this.categoryName = categoryName;
        this.icon = icon;
    }
    
    // Builder 패턴
    public static CategoryDtoBuilder builder() {
        return new CategoryDtoBuilder();
    }
    
    // Getter/Setter 메서드들
    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }
    
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    
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