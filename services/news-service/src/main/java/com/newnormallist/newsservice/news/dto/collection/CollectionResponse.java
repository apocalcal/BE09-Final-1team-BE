package com.newnormallist.newsservice.news.dto.collection;

import com.newnormallist.newsservice.news.entity.ScrapStorage;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CollectionResponse {
    private Integer id;
    private String name;
    private LocalDateTime createdAt;

    // ScrapStorage 엔티티를 CollectionResponse DTO로 변환하는 정적 메서드
    public static CollectionResponse from(ScrapStorage scrapStorage) {
        return new CollectionResponse(
                scrapStorage.getStorageId(),
                scrapStorage.getStorageName(),
                scrapStorage.getCreatedAt()
        );
    }
}
