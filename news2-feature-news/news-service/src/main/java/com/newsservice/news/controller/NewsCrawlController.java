package com.newsservice.news.controller;

import com.newsservice.news.dto.response.NewsCrawlDto;
import com.newsservice.news.dto.response.NewsPreviewDto;
import com.newsservice.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
public class NewsCrawlController {

    private final NewsService newsService;


    // 크롤러에서 전송된 뉴스 데이터를 받아 저장
    @PostMapping("/crawl")
    public ResponseEntity<Void> saveCrawledNews(@RequestBody NewsCrawlDto dto) {
        newsService.saveCrawledNews(dto);
        return ResponseEntity.ok().build();
    }
    // 크롤링된 뉴스 미리보기 (저장하지 않고 미리보기만)
//    @PostMapping("/crawl/preview")
//    public ResponseEntity<NewsPreviewDto> previewCrawledNews(@RequestBody NewsCrawlDto dto) {
//        NewsPreviewDto preview = newsService.previewCrawledNews(dto);
//        return ResponseEntity.ok(preview);
//    }

}
