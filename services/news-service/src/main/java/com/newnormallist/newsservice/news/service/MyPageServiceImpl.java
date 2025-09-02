package com.newnormallist.newsservice.news.service;

import com.newnormallist.newsservice.news.dto.NewsListResponse;
import com.newnormallist.newsservice.news.entity.Category;
import com.newnormallist.newsservice.news.entity.News;
import com.newnormallist.newsservice.news.entity.NewsScrap;
import com.newnormallist.newsservice.news.entity.ScrapStorage;
import com.newnormallist.newsservice.news.exception.ResourceNotFoundException;
import com.newnormallist.newsservice.news.repository.NewsScrapRepository;
import com.newnormallist.newsservice.news.repository.ScrapStorageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageServiceImpl implements MyPageService {

    private final ScrapStorageRepository scrapStorageRepository;
    private final NewsScrapRepository newsScrapRepository;

    @Override
    public Page<NewsListResponse> getScrappedNews(Long userId, String category, Pageable pageable) {
        Page<NewsScrap> scrapsPage;

        if (category == null || category.isEmpty() || category.equalsIgnoreCase("전체")) {
            scrapsPage = newsScrapRepository.findByUserId(userId, pageable);
        } else {
            scrapsPage = newsScrapRepository.findByUserId(userId, pageable);
            Category categoryEnum = Arrays.stream(Category.values())
                    .filter(c -> c.getCategoryName().equalsIgnoreCase(category))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("No enum constant for category name: " + category));
            scrapsPage = new PageImpl<>(scrapsPage.getContent().stream()
                    .filter(scrap -> scrap.getNews().getCategoryName().equals(categoryEnum))
                    .collect(Collectors.toList()), pageable, scrapsPage.getTotalElements());
        }

        List<NewsListResponse> dtoList = scrapsPage.getContent().stream()
                .map(this::convertToNewsListResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(dtoList, pageable, scrapsPage.getTotalElements());
    }

    @Override
    public void deleteScrap(Long userId, Long newsId) {
        NewsScrap newsScrap = newsScrapRepository.findByUserIdAndNewsNewsId(userId, newsId)
                .orElseThrow(() -> new IllegalStateException("스크랩된 뉴스를 찾을 수 없습니다."));
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

            String categoryName = (news.getCategoryName() != null) ? news.getCategoryName().getCategoryName() : "기타";

            return NewsListResponse.builder()
                    .newsId(news.getNewsId())
                    .title(news.getTitle())
                    .press(news.getPress())
                    .categoryName(categoryName)
                    .imageUrl(news.getImageUrl())
                    .build();

        } catch (Exception e) {
            log.error("Failed to convert NewsScraper with ID: {}. Error: {}", newsScrap.getScrapId(), e.getMessage(), e);
            return NewsListResponse.builder()
                    .newsId(0L)
                    .title("[데이터 변환 오류]")
                    .press("-")
                    .categoryName("오류")
                    .build();
        }
    }
}
