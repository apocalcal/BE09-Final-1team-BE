package com.newnormallist.newsservice.service;

import com.newnormallist.newsservice.dto.NewsDetailResponseDto;
import com.newnormallist.newsservice.dto.NewsListResponseDto;
import com.newnormallist.newsservice.entity.News;
import com.newnormallist.newsservice.exception.NewsNotFoundException;
import com.newnormallist.newsservice.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {

    private final NewsRepository newsRepository;

    /**
     * 뉴스 상세 정보 조회
     * @param newsId 뉴스 ID
     * @return 뉴스 상세 정보 DTO
     */
    @Transactional(readOnly = true)
    public NewsDetailResponseDto getNewsDetail(Long newsId) {
        log.info("뉴스 상세 정보 조회 시작 - newsId: {}", newsId);

        News news = newsRepository.findByNewsId(newsId)
                .orElseThrow(() -> new NewsNotFoundException("뉴스를 찾을 수 없습니다. newsId: " + newsId));

        log.info("뉴스 상세 정보 조회 완료 - newsId: {}, title: {}", newsId, news.getTitle());
        return NewsDetailResponseDto.from(news);
    }

    /**
     * 모든 뉴스 목록 조회 (페이징)
     * @param page 페이지 번호 (0부터 시작)
     * @param size 페이지 크기
     * @return 뉴스 목록 페이지
     */
    @Transactional(readOnly = true)
    public Page<NewsListResponseDto> getAllNews(int page, int size) {
        log.info("뉴스 목록 조회 시작 - page: {}, size: {}", page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<News> newsPage = newsRepository.findAllByOrderByNewsIdDesc(pageable);

        Page<NewsListResponseDto> responsePage = newsPage.map(NewsListResponseDto::from);

        log.info("뉴스 목록 조회 완료 - 총 {}페이지 중 {}페이지, {}개 뉴스", 
                newsPage.getTotalPages(), page + 1, responsePage.getContent().size());
        
        return responsePage;
    }

    /**
     * 카테고리별 뉴스 목록 조회 (페이징)
     * @param categoryType 카테고리
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 뉴스 목록 페이지
     */
    @Transactional(readOnly = true)
    public Page<NewsListResponseDto> getNewsByCategory(News.CategoryType categoryType, int page, int size) {
        log.info("카테고리별 뉴스 목록 조회 시작 - category: {}, page: {}, size: {}", categoryType, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<News> newsPage = newsRepository.findByCategoryNameOrderByCreatedAtDescNewsIdDesc(categoryType, pageable);

        Page<NewsListResponseDto> responsePage = newsPage.map(NewsListResponseDto::from);

        log.info("카테고리별 뉴스 목록 조회 완료 - category: {}, {}개 뉴스", categoryType, responsePage.getContent().size());
        
        return responsePage;
    }

    /**
     * 제목으로 뉴스 검색 (페이징)
     * @param keyword 검색 키워드
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 검색된 뉴스 목록 페이지
     */
    @Transactional(readOnly = true)
    public Page<NewsListResponseDto> searchNewsByTitle(String keyword, int page, int size) {
        log.info("뉴스 제목 검색 시작 - keyword: {}, page: {}, size: {}", keyword, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<News> newsPage = newsRepository.findByTitleContaining(keyword, pageable);

        Page<NewsListResponseDto> responsePage = newsPage.map(NewsListResponseDto::from);

        log.info("뉴스 제목 검색 완료 - keyword: {}, {}개 뉴스", keyword, responsePage.getContent().size());
        
        return responsePage;
    }

    /**
     * 언론사별 뉴스 조회 (페이징)
     * @param press 언론사
     * @param page 페이지 번호
     * @param size 페이지 크기
     * @return 뉴스 목록 페이지
     */
    @Transactional(readOnly = true)
    public Page<NewsListResponseDto> getNewsByPress(String press, int page, int size) {
        log.info("언론사별 뉴스 조회 시작 - press: {}, page: {}, size: {}", press, page, size);

        Pageable pageable = PageRequest.of(page, size);
        Page<News> newsPage = newsRepository.findByPressOrderByCreatedAtDescNewsIdDesc(press, pageable);

        Page<NewsListResponseDto> responsePage = newsPage.map(NewsListResponseDto::from);

        log.info("언론사별 뉴스 조회 완료 - press: {}, {}개 뉴스", press, responsePage.getContent().size());
        
        return responsePage;
    }

    /**
     * 최신 뉴스 조회
     * @param limit 조회할 뉴스 개수
     * @return 최신 뉴스 목록
     */
    @Transactional(readOnly = true)
    public List<NewsListResponseDto> getLatestNews(int limit) {
        log.info("최신 뉴스 조회 시작 - limit: {}", limit);

        Pageable pageable = PageRequest.of(0, limit);
        List<News> newsList = newsRepository.findLatestNews(pageable);

        List<NewsListResponseDto> responseList = newsList.stream()
                .map(NewsListResponseDto::from)
                .collect(Collectors.toList());

        log.info("최신 뉴스 조회 완료 - {}개 뉴스", responseList.size());
        return responseList;
    }

    /**
     * 발행일 기간으로 뉴스 조회
     * @param startDate 시작일
     * @param endDate 종료일
     * @return 뉴스 목록
     */
    @Transactional(readOnly = true)
    public List<NewsListResponseDto> getNewsByDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        log.info("기간별 뉴스 조회 시작 - startDate: {}, endDate: {}", startDate, endDate);

        List<News> newsList = newsRepository.findByPublishedAtBetween(startDate, endDate);

        List<NewsListResponseDto> responseList = newsList.stream()
                .map(NewsListResponseDto::from)
                .collect(Collectors.toList());

        log.info("기간별 뉴스 조회 완료 - {}개 뉴스", responseList.size());
        return responseList;
    }

    /**
     * 뉴스 개수 조회
     * @return 총 뉴스 개수
     */
    @Transactional(readOnly = true)
    public Long getNewsCount() {
        log.info("뉴스 개수 조회 시작");

        Long count = newsRepository.count();

        log.info("뉴스 개수 조회 완료 - 총 {}개", count);
        return count;
    }

    /**
     * 카테고리별 뉴스 개수 조회
     * @param categoryType 카테고리
     * @return 해당 카테고리의 뉴스 개수
     */
    @Transactional(readOnly = true)
    public Long getNewsCountByCategory(News.CategoryType categoryType) {
        log.info("카테고리별 뉴스 개수 조회 시작 - category: {}", categoryType);

        Pageable pageable = PageRequest.of(0, 1);
        Page<News> newsPage = newsRepository.findByCategoryNameOrderByCreatedAtDescNewsIdDesc(categoryType, pageable);
        Long count = newsPage.getTotalElements();

        log.info("카테고리별 뉴스 개수 조회 완료 - category: {}, {}개", categoryType, count);
        return count;
    }

    /**
     * 중복 제거 상태별 뉴스 조회
     * @param dedupState 중복 제거 상태
     * @return 뉴스 목록
     */
    @Transactional(readOnly = true)
    public List<NewsListResponseDto> getNewsByDedupState(News.DedupState dedupState) {
        log.info("중복제거 상태별 뉴스 조회 시작 - dedupState: {}", dedupState);

        List<News> newsList = newsRepository.findByDedupState(dedupState);

        List<NewsListResponseDto> responseList = newsList.stream()
                .map(NewsListResponseDto::from)
                .collect(Collectors.toList());

        log.info("중복제거 상태별 뉴스 조회 완료 - dedupState: {}, {}개 뉴스", dedupState, responseList.size());
        return responseList;
    }
}
