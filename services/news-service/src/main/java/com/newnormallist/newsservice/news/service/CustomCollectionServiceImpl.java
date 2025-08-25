package com.newnormallist.newsservice.news.service;

import com.newnormallist.newsservice.news.entity.News;
import com.newnormallist.newsservice.news.entity.NewsScrap;
import com.newnormallist.newsservice.news.entity.ScrapStorage;
import com.newnormallist.newsservice.news.dto.NewsListResponse;
import com.newnormallist.newsservice.news.dto.collection.CollectionResponse;
import com.newnormallist.newsservice.news.exception.CollectionNotFoundException;
import com.newnormallist.newsservice.news.exception.ForbiddenAccessException;
import com.newnormallist.newsservice.news.exception.NewsNotFoundException;
import com.newnormallist.newsservice.news.repository.NewsRepository;
import com.newnormallist.newsservice.news.repository.NewsScrapRepository;
import com.newnormallist.newsservice.news.repository.ScrapStorageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CustomCollectionServiceImpl implements CustomCollectionService {

    private final ScrapStorageRepository scrapStorageRepository;
    private final NewsScrapRepository newsScrapRepository;
    private final NewsRepository newsRepository;

    @Override
    public CollectionResponse createCollection(Long userId, String name) {
        ScrapStorage newStorage = new ScrapStorage();
        newStorage.setUserId(userId);
        newStorage.setStorageName(name);
        newStorage.setCreatedAt(LocalDateTime.now());
        newStorage.setUpdatedAt(LocalDateTime.now());
        ScrapStorage savedStorage = scrapStorageRepository.save(newStorage);
        return CollectionResponse.from(savedStorage);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CollectionResponse> getUserCollections(Long userId) {
        return scrapStorageRepository.findByUserId(userId).stream()
                .map(CollectionResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public void addNewsToCollection(Long userId, Long collectionId, Long newsId) {
        ScrapStorage storage = getScrapStorageAndVerifyOwner(userId, collectionId);
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsNotFoundException("뉴스를 찾을 수 없습니다: " + newsId));

        // 이미 스크랩된 뉴스인지 확인
        if (newsScrapRepository.findByStorageIdAndNewsNewsId(storage.getStorageId(), newsId).isPresent()) {
            return; // 이미 존재하면 아무 작업도 하지 않음
        }

        NewsScrap newsScrap = new NewsScrap();
        newsScrap.setStorageId(storage.getStorageId()); // setStorage -> setStorageId
        newsScrap.setNews(news);
        newsScrap.setCreatedAt(LocalDateTime.now());
        newsScrap.setUpdatedAt(LocalDateTime.now());
        newsScrapRepository.save(newsScrap);
    }

    @Override
    public void deleteNewsFromCollection(Long userId, Long collectionId, Long newsId) {
        ScrapStorage storage = getScrapStorageAndVerifyOwner(userId, collectionId);
        NewsScrap newsScrap = newsScrapRepository.findByStorageIdAndNewsNewsId(storage.getStorageId(), newsId)
                .orElseThrow(() -> new NewsNotFoundException("컬렉션에서 해당 뉴스를 찾을 수 없습니다."));

        newsScrapRepository.delete(newsScrap);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NewsListResponse> getNewsInCollection(Long userId, Long collectionId, Pageable pageable) {
        ScrapStorage storage = getScrapStorageAndVerifyOwner(userId, collectionId);
        Page<NewsScrap> scraps = newsScrapRepository.findByStorageId(storage.getStorageId(), pageable);
        return scraps.map(scrap -> NewsListResponse.from(scrap.getNews()));
    }

    @Override
    public void deleteCollection(Long userId, Long collectionId) {
        ScrapStorage storage = getScrapStorageAndVerifyOwner(userId, collectionId);
        // 해당 컬렉션에 속한 모든 스크랩(news_scrap) 정보를 먼저 삭제
        newsScrapRepository.deleteByStorageId(storage.getStorageId());
        // 컬렉션(scrap_storage) 삭제
        scrapStorageRepository.delete(storage);
    }

    private ScrapStorage getScrapStorageAndVerifyOwner(Long userId, Long collectionId) {
        ScrapStorage storage = scrapStorageRepository.findById(collectionId.intValue()) // Long -> Integer
                .orElseThrow(() -> new CollectionNotFoundException("컬렉션을 찾을 수 없습니다: " + collectionId));
        if (!storage.getUserId().equals(userId)) {
            throw new ForbiddenAccessException("해당 컬렉션에 접근할 권한이 없습니다.");
        }
        return storage;
    }
}
