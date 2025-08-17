package com.newsletterservice.service;

import com.newsletterservice.dto.NewsletterCreateRequest;
import com.newsletterservice.entity.NewsCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentGenerationService {
    
    public String generateContent(NewsletterCreateRequest request) {
        // 사용자 정의 콘텐츠가 있으면 사용
        if (request.getCustomContent() != null && !request.getCustomContent().trim().isEmpty()) {
            return request.getCustomContent();
        }
        
        // 기본 콘텐츠 생성
        StringBuilder content = new StringBuilder();
        content.append("<h1>").append(request.getTitle()).append("</h1>\n");
        content.append("<p>안녕하세요! 오늘의 뉴스레터입니다.</p>\n");
        
        // 카테고리별 섹션 생성
        Set<NewsCategory> categories = request.getCategories();
        for (NewsCategory category : categories) {
            content.append("<h2>").append(getCategoryKoreanName(category)).append("</h2>\n");
            content.append("<p>").append(getCategoryKoreanName(category)).append(" 관련 최신 뉴스를 확인해보세요.</p>\n");
        }
        
        content.append("<p>더 많은 뉴스는 웹사이트에서 확인하실 수 있습니다.</p>\n");
        content.append("<p>감사합니다.</p>");
        
        return content.toString();
    }
    
    private String getCategoryKoreanName(NewsCategory category) {
        switch (category) {
            case POLITICS: return "정치";
            case ECONOMY: return "경제";
            case SOCIETY: return "사회";
            case CULTURE: return "문화";
            case SPORTS: return "스포츠";
            case TECHNOLOGY: return "기술";
            case ENTERTAINMENT: return "연예";
            case INTERNATIONAL: return "국제";
            default: return category.name();
        }
    }
}
