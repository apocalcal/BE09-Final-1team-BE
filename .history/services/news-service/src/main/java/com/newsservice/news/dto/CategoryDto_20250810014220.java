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
        private String categoryCode;
        private String categoryName;
        private String icon;
        
        public CategoryDtoBuilder categoryCode(String categoryCode) {
            this.categoryCode = categoryCode;
            return this;
        }
        
        public CategoryDtoBuilder categoryName(String categoryName) {
            this.categoryName = categoryName;
            return this;
        }
        
        public CategoryDtoBuilder icon(String icon) {
            this.icon = icon;
            return this;
        }
        
        public CategoryDto build() {
            return new CategoryDto(categoryCode, categoryName, icon);
        }
    }
} 