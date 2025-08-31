package com.newnormallist.newsservice.news.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScrapStorageResponse {
    private Integer storageId;
    private String storageName;
    private long newsCount; // 뉴스 개수 필드 추가
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
