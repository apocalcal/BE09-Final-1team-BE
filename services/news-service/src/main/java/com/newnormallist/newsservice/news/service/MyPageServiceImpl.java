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
        log.info("사용자 ID {}의 스크랩 목록 조회 시작. 카테고리: {}, 페이지: {}", userId, category, pageable.getPageNumber());

        List<ScrapStorage> scrapStorages = scrapStorageRepository.findByUserId(userId);
        if (scrapStorages.isEmpty()) {
            throw new ResourceNotFoundException("해당 사용자의 스크랩 보관함을 찾을 수 없습니다: " + userId);
        }
        ScrapStorage scrapStorage = scrapStorages.get(0);
        log.debug("기본 스크랩 보관함 찾음: {}", scrapStorage.getStorageId());

        Page<NewsScrap> scrapsPage;

        if (category == null || category.trim().isEmpty() || "전체".equals(category)) {
            log.info("카테고리 필터 없음. 전체 스크랩을 조회합니다.");
            scrapsPage = newsScrapRepository.findByStorageIdWithNews(scrapStorage.getStorageId(), pageable);
        } else {
            log.info("카테고리 필터 적용: {}", category);
            try {
                Category categoryEnum = Arrays.stream(Category.values())
                        .filter(c -> c.getCategoryName().equals(category))
                        .findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다: " + category));
                log.info("카테고리 '{}'를 Enum '{}'(으)로 변환 성공.", category, categoryEnum.name());
                
                scrapsPage = newsScrapRepository.findByStorageIdAndNewsCategoryNameWithNews(scrapStorage.getStorageId(), categoryEnum, pageable);
                log.info("카테고리별 스크랩 조회 완료. 총 {} 페이지, {}개의 스크랩 발견.", scrapsPage.getTotalPages(), scrapsPage.getTotalElements());

            } catch (IllegalArgumentException e) {
                log.error("잘못된 카테고리 값: {}", category, e);
                return Page.empty(pageable);
            }
        }

        List<NewsListResponse> dtoList = new ArrayList<>();
        for (NewsScrap scrap : scrapsPage.getContent()) {
            if (scrap != null) { // null인 객체를 명시적으로 건너뜁니다.
                dtoList.add(convertToNewsListResponse(scrap));
            }
        }
        
        log.info("스크랩 목록 DTO 변환 완료. {}개의 항목 반환.", dtoList.size());
        return new PageImpl<>(dtoList, pageable, scrapsPage.getTotalElements());
    }

    @Override
    @Transactional
    public void deleteScrap(Long userId, Long newsId) {
        List<ScrapStorage> scrapStorages = scrapStorageRepository.findByUserId(userId);
        if (scrapStorages.isEmpty()) {
            throw new ResourceNotFoundException("해당 사용자의 스크랩 보관함을 찾을 수 없습니다: " + userId);
        }
        ScrapStorage scrapStorage = scrapStorages.get(0);

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
