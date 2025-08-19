package com.newnormalist.newsservice.news.service;

import com.newnormalist.newsservice.news.dto.RelatedNewsResponseDto;
import com.newnormalist.newsservice.news.entity.News;
import com.newnormalist.newsservice.news.exception.NewsNotFoundException;
import com.newnormalist.newsservice.news.repository.NewsRepository;
import com.newnormalist.newsservice.news.repository.RelatedNewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RelatedNewsServiceImpl implements RelatedNewsService {

    private final NewsRepository newsRepository;
    private final RelatedNewsRepository relatedNewsRepository;
    private static final int MAX_RELATED_NEWS = 4;

    @Override
    public List<RelatedNewsResponseDto> getRelatedNews(Long newsId) {
        // 1. 뉴스 조회
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsNotFoundException("뉴스를 찾을 수 없습니다: " + newsId));

        List<News> relatedNewsList = new ArrayList<>();

        // 2. dedup_state에 따른 연관뉴스 조회 로직
        switch (news.getDedupState()) {
            case REPRESENTATIVE:
                relatedNewsList = getRelatedNewsForRepresentative(news);
                break;
            case KEPT:
                relatedNewsList = getRelatedNewsForKept(news);
                break;
            case RELATED:
                relatedNewsList = getRelatedNewsForRelated(news);
                break;
        }

        // 3. DTO 변환 및 반환
        return relatedNewsList.stream()
                .map(RelatedNewsResponseDto::from)
                .collect(Collectors.toList());
    }

    /**
     * REPRESENTATIVE 상태의 뉴스에 대한 연관뉴스 조회
     */
    private List<News> getRelatedNewsForRepresentative(News news) {
        List<News> relatedNewsList = new ArrayList<>();

        // 1. related_news 테이블에서 rep_oid_aid가 해당 뉴스의 oid_aid인 related_oid_aid들 조회
        List<String> relatedOidAids = relatedNewsRepository.findRelatedOidAidsByRepOidAid(news.getOidAid());
        
        if (!relatedOidAids.isEmpty()) {
            // 2. 해당 related_oid_aid를 가진 뉴스들 조회
            List<News> relatedNews = newsRepository.findByOidAidIn(relatedOidAids);
            
            // 3. 4개 이상이면 랜덤으로 4개 선택
            if (relatedNews.size() >= MAX_RELATED_NEWS) {
                Collections.shuffle(relatedNews);
                relatedNewsList = relatedNews.subList(0, MAX_RELATED_NEWS);
            } else {
                relatedNewsList.addAll(relatedNews);
            }
        }

        // 4. 4개 미만이면 같은 created_at, 같은 category_name인 뉴스로 채우기
        if (relatedNewsList.size() < MAX_RELATED_NEWS) {
            int remainingCount = MAX_RELATED_NEWS - relatedNewsList.size();
            List<News> additionalNews = getNewsBySameTimeAndCategory(news, relatedNewsList, remainingCount);
            relatedNewsList.addAll(additionalNews);
        }

        return relatedNewsList;
    }

    /**
     * KEPT 상태의 뉴스에 대한 연관뉴스 조회
     */
    private List<News> getRelatedNewsForKept(News news) {
        // 같은 created_at, 같은 category_name인 뉴스를 랜덤으로 4개 조회 (해당 뉴스 제외)
        List<News> sameTimeCategoryNews = newsRepository.findByCreatedAtAndCategoryNameAndNewsIdNot(
                news.getCreatedAt(), news.getCategoryName(), news.getNewsId());

        if (sameTimeCategoryNews.size() >= MAX_RELATED_NEWS) {
            Collections.shuffle(sameTimeCategoryNews);
            return sameTimeCategoryNews.subList(0, MAX_RELATED_NEWS);
        } else {
            return sameTimeCategoryNews;
        }
    }

    /**
     * RELATED 상태의 뉴스에 대한 연관뉴스 조회
     */
    private List<News> getRelatedNewsForRelated(News news) {
        List<News> relatedNewsList = new ArrayList<>();

        // 1. 해당 뉴스의 oid_aid가 related_oid_aid인 행의 rep_oid_aid 조회
        String repOidAid = relatedNewsRepository.findRepOidAidByRelatedOidAid(news.getOidAid());
        
        if (repOidAid != null) {
            // 2. rep_oid_aid를 oid_aid로 가지는 뉴스와 연관관계인 뉴스들 조회
            List<String> relatedOidAids = relatedNewsRepository.findRelatedOidAidsByRepOidAid(repOidAid);
            
            if (!relatedOidAids.isEmpty()) {
                // 3. 해당 related_oid_aid를 가진 뉴스들 조회 (현재 뉴스 제외)
                List<News> relatedNews = newsRepository.findByOidAidInAndNewsIdNot(relatedOidAids, news.getNewsId());
                
                if (relatedNews.size() >= MAX_RELATED_NEWS) {
                    Collections.shuffle(relatedNews);
                    relatedNewsList = relatedNews.subList(0, MAX_RELATED_NEWS);
                } else {
                    relatedNewsList.addAll(relatedNews);
                }
            }
        }

        // 4. 4개 미만이면 같은 created_at, 같은 category_name인 뉴스로 채우기
        if (relatedNewsList.size() < MAX_RELATED_NEWS) {
            int remainingCount = MAX_RELATED_NEWS - relatedNewsList.size();
            List<News> additionalNews = getNewsBySameTimeAndCategory(news, relatedNewsList, remainingCount);
            relatedNewsList.addAll(additionalNews);
        }

        return relatedNewsList;
    }

    /**
     * 같은 시간대와 카테고리의 뉴스를 조회하여 추가
     */
    private List<News> getNewsBySameTimeAndCategory(News news, List<News> excludeNews, int count) {
        // 이미 선택된 뉴스들의 ID 목록
        List<Long> excludeNewsIds = excludeNews.stream()
                .map(News::getNewsId)
                .collect(Collectors.toList());
        excludeNewsIds.add(news.getNewsId()); // 현재 뉴스도 제외

        // 같은 날짜, 같은 카테고리, 같은 시간대(오전/오후)인 뉴스 조회
        LocalDateTime startOfDay = news.getCreatedAt().toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1);
        
        List<News> sameDayCategoryNews = newsRepository.findByCreatedAtBetweenAndCategoryNameAndNewsIdNotIn(
                startOfDay, endOfDay, news.getCategoryName(), excludeNewsIds);

        // 오전/오후 시간대 필터링
        LocalTime newsTime = news.getCreatedAt().toLocalTime();
        boolean isMorning = newsTime.isBefore(LocalTime.NOON);
        
        List<News> filteredNews = sameDayCategoryNews.stream()
                .filter(n -> {
                    LocalTime time = n.getCreatedAt().toLocalTime();
                    boolean newsIsMorning = time.isBefore(LocalTime.NOON);
                    return isMorning == newsIsMorning;
                })
                .collect(Collectors.toList());

        if (filteredNews.size() >= count) {
            Collections.shuffle(filteredNews);
            return filteredNews.subList(0, count);
        } else {
            return filteredNews;
        }
    }
}
