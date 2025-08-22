package com.newsletterservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class NewsResponse {
    private Long id;
    private String title;
    private String content;
    private String summary;
    private String imageUrl;
    private String sourceUrl;
    private String category;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
}
