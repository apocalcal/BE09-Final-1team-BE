package com.newsservice.news.dto;

import com.newsservice.news.entity.Category;
import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NewsCrawlDto {
    
    private Long linkId;
    private String title;
    private String press;
    private String content;
    private String reporterName;
    private LocalDateTime publishedAt;
    private Category category;

}