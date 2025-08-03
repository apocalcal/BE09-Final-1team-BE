package com.newsservice.news.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsCrawlDto {
    
    private String linkId;
    private String title;
    private String press;
    private String content;
    private String reporterName;
    private LocalDateTime publishedAt;
    private Integer categoryId;
} 