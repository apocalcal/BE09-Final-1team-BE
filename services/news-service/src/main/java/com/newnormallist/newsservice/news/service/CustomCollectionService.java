package com.newnormallist.newsservice.news.service;

import com.newnormallist.newsservice.news.dto.collection.CollectionResponse;
import com.newnormallist.newsservice.news.dto.NewsListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomCollectionService {
    CollectionResponse createCollection(Long userId, String name);
    List<CollectionResponse> getUserCollections(Long userId);
    void addNewsToCollection(Long userId, Long collectionId, Long newsId);
    void deleteNewsFromCollection(Long userId, Long collectionId, Long newsId);
    Page<NewsListResponse> getNewsInCollection(Long userId, Long collectionId, Pageable pageable);
    void deleteCollection(Long userId, Long collectionId);
}
