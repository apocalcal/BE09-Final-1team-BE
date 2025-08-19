package com.newsservice.news.service;

import com.newsservice.news.dto.NewsListResponse;
import com.newsservice.news.entity.News;
import com.newsservice.news.entity.NewsScrap;
import com.newsservice.news.entity.ScrapStorage;
import com.newsservice.news.exception.ResourceNotFoundException;
import com.newsservice.news.repository.NewsScrapRepository;
import com.newsservice.news.repository.ScrapStorageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageServiceImpl implements MyPageService {

    private final ScrapStorageRepository scrapStorageRepository;
    private final NewsScrapRepository newsScrapRepository;

    @Override
    public Page<NewsListResponse> getScrappedNews(Long userId, Pageable pageable) {
        ScrapStorage scrapStorage = scrapStorageRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 사용자의 스크랩 보관함을 찾을 수 없습니다: " + userId));

        Page<NewsScrap> scrapsPage = newsScrapRepository.findByStorageIdWithNews(scrapStorage.getStorageId(), pageable);

        List<NewsListResponse> dtoList = scrapsPage.getContent().stream()
                .map(this::convertToNewsListResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, scrapsPage.getTotalElements());
    }

    @Override
    @Transactional
    public void deleteScrap(Long userId, Long newsId) {
        ScrapStorage scrapStorage = scrapStorageRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 사용자의 스크랩 보관함을 찾을 수 없습니다: " + userId));

        NewsScrap newsScrap = newsScrapRepository.findByStorageIdAndNewsNewsId(scrapStorage.getStorageId(), newsId)
                .orElseThrow(() -> new ResourceNotFoundException("해당 뉴스는 스크랩 목록에 없습니다: " + newsId));

        newsScrapRepository.delete(newsScrap);
    }

    private NewsListResponse convertToNewsListResponse(NewsScrap newsScrap) {
        try {
            News news = newsScrap.getNews();

            if (news == null) {
                log.warn("Scrap ID: {} is pointing to a deleted News entity. Returning a placeholder.", newsScrap.getScrapId());
                return NewsListResponse.builder()
                        .newsId(0L)
                        .title("[삭제된 뉴스]")
                        .press("-")
                        .categoryName("기타")
                        .build();
            }

            String categoryName = (news.getCategoryName() != null) ? news.getCategoryName().name() : "기타";

            return NewsListResponse.builder()
                    .newsId(news.getNewsId())
                    .title(news.getTitle())
                    .press(news.getPress())
                    .categoryName(categoryName)
                    .imageUrl(news.getImageUrl())
                    .build();

        } catch (Exception e) {
            log.error("Failed to convert NewsScrap with ID: {}. Error: {}", newsScrap.getScrapId(), e.getMessage(), e);
            return NewsListResponse.builder()
                    .newsId(0L)
                    .title("[데이터 변환 오류]")
                    .press("-")
                    .categoryName("오류")
                    .build();
        }
    }
}
