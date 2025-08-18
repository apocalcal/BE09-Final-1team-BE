package com.newsletterservice.service;

import com.newsletterservice.client.NewsServiceClient;
import com.newsletterservice.client.UserServiceClient;
import com.newsletterservice.client.dto.CategoryResponse;
import com.newsletterservice.client.dto.NewsResponse;
import com.newsletterservice.client.dto.UserResponse;
import com.newsletterservice.common.ApiResponse;
import com.newsletterservice.dto.NewsletterCreateRequest;
import com.newsletterservice.entity.NewsCategory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContentGenerationService {
    
    private final NewsServiceClient newsServiceClient;
    private final UserServiceClient userServiceClient;
    
    public String generateContent(NewsletterCreateRequest request) {
        try {
            // 실제 최신 뉴스 데이터 가져오기
            ApiResponse<List<NewsResponse>> latestNewsResponse = newsServiceClient.getLatestNews(null, 5);
            List<NewsResponse> latestNews = latestNewsResponse.getData();
            
            StringBuilder content = new StringBuilder();
            content.append("<h1>뉴스레터 #").append(request.getNewsletterId()).append("</h1>\n");
            content.append("<p>안녕하세요! 오늘의 뉴스레터입니다.</p>\n");
            
            // 개인화 여부에 따른 내용 추가
            if (request.isPersonalized()) {
                content.append("<p>이 뉴스레터는 개인화된 내용으로 구성되었습니다.</p>\n");
            }
            
            // 실제 뉴스 데이터로 섹션 생성
            if (latestNews != null && !latestNews.isEmpty()) {
                content.append("<h2>📰 최신 뉴스</h2>\n");
                for (NewsResponse news : latestNews) {
                    content.append("<div style='margin-bottom: 20px; padding: 15px; border: 1px solid #eee; border-radius: 8px;'>\n");
                    content.append("<h3>").append(news.getTitle()).append("</h3>\n");
                    content.append("<p><strong>카테고리:</strong> ").append(news.getCategory()).append("</p>\n");
                    if (news.getSummary() != null && !news.getSummary().isEmpty()) {
                        content.append("<p>").append(news.getSummary()).append("</p>\n");
                    }
                    content.append("<p><small>작성일: ").append(news.getCreatedAt()).append("</small></p>\n");
                    content.append("</div>\n");
                }
            } else {
                // 뉴스 데이터가 없을 경우 기본 카테고리 정보 표시
                List<NewsCategory> defaultCategories = Arrays.asList(
                    NewsCategory.POLITICS, 
                    NewsCategory.ECONOMY, 
                    NewsCategory.SOCIETY
                );
                
                for (NewsCategory category : defaultCategories) {
                    content.append("<h2>").append(category.getCategoryName()).append(" ").append(category.getIcon()).append("</h2>\n");
                    content.append("<p>").append(category.getCategoryName()).append(" 관련 최신 뉴스를 확인해보세요.</p>\n");
                }
            }
            
            content.append("<p>더 많은 뉴스는 웹사이트에서 확인하실 수 있습니다.</p>\n");
            content.append("<p>감사합니다.</p>");
            
            return content.toString();
        } catch (Exception e) {
            log.error("뉴스 데이터 조회 중 오류 발생", e);
            return generateFallbackContent(request);
        }
    }
    
    /**
     * 특정 사용자들을 위한 개인화된 콘텐츠 생성
     */
    public String generatePersonalizedContent(NewsletterCreateRequest request, List<Long> userIds) {
        try {
            StringBuilder content = new StringBuilder();
            content.append("<h1>개인화된 뉴스레터 #").append(request.getNewsletterId()).append("</h1>\n");
            content.append("<p>안녕하세요! 귀하를 위한 맞춤 뉴스레터입니다.</p>\n");
            
            // 사용자 정보 가져오기
            if (userIds != null && !userIds.isEmpty()) {
                ApiResponse<List<UserResponse>> usersResponse = userServiceClient.getUsersByIds(userIds);
                List<UserResponse> users = usersResponse.getData();
                
                if (users != null && !users.isEmpty()) {
                    content.append("<h2>👥 구독자 정보</h2>\n");
                    content.append("<p>총 ").append(users.size()).append("명의 구독자에게 발송됩니다.</p>\n");
                }
            }
            
            // 사용자별 맞춤 뉴스 생성
            content.append("<h2>🎯 맞춤 추천 뉴스</h2>\n");
            content.append("<p>관심사와 읽기 패턴을 분석하여 추천하는 뉴스입니다.</p>\n");
            
            // 트렌딩 뉴스 가져오기
            ApiResponse<List<NewsResponse>> trendingNewsResponse = newsServiceClient.getTrendingNews(24, 3);
            List<NewsResponse> trendingNews = trendingNewsResponse.getData();
            
            if (trendingNews != null && !trendingNews.isEmpty()) {
                content.append("<h3>🔥 트렌딩 뉴스</h3>\n");
                for (NewsResponse news : trendingNews) {
                    content.append("<div style='margin-bottom: 15px; padding: 10px; background-color: #f8f9fa; border-radius: 5px;'>\n");
                    content.append("<h4>").append(news.getTitle()).append("</h4>\n");
                    content.append("<p><strong>카테고리:</strong> ").append(news.getCategory()).append("</p>\n");
                    if (news.getSummary() != null && !news.getSummary().isEmpty()) {
                        content.append("<p>").append(news.getSummary()).append("</p>\n");
                    }
                    content.append("</div>\n");
                }
            }
            
            // 카테고리별 최신 뉴스
            List<NewsCategory> categories = Arrays.asList(
                NewsCategory.POLITICS, 
                NewsCategory.ECONOMY, 
                NewsCategory.SOCIETY,
                NewsCategory.IT_SCIENCE
            );
            
            for (NewsCategory category : categories) {
                try {
                    ApiResponse<List<NewsResponse>> categoryNewsResponse = newsServiceClient.getNewsByCategory(
                        category.getCategoryName(), 0, 2);
                    List<NewsResponse> categoryNews = categoryNewsResponse.getData();
                    
                    content.append("<h3>").append(category.getCategoryName()).append(" ").append(category.getIcon()).append("</h3>\n");
                    
                    if (categoryNews != null && !categoryNews.isEmpty()) {
                        for (NewsResponse news : categoryNews) {
                            content.append("<div style='margin-bottom: 10px; padding: 8px; border-left: 3px solid #007bff;'>\n");
                            content.append("<h4>").append(news.getTitle()).append("</h4>\n");
                            if (news.getSummary() != null && !news.getSummary().isEmpty()) {
                                content.append("<p>").append(news.getSummary()).append("</p>\n");
                            }
                            content.append("</div>\n");
                        }
                    } else {
                        content.append("<p>").append(category.getCategoryName()).append(" 분야의 최신 소식을 전해드립니다.</p>\n");
                    }
                } catch (Exception e) {
                    log.warn("카테고리 {} 뉴스 조회 실패", category.getCategoryName(), e);
                    content.append("<p>").append(category.getCategoryName()).append(" 분야의 최신 소식을 전해드립니다.</p>\n");
                }
            }
            
            content.append("<p>더 많은 맞춤 뉴스는 웹사이트에서 확인하실 수 있습니다.</p>\n");
            content.append("<p>감사합니다.</p>");
            
            return content.toString();
        } catch (Exception e) {
            log.error("개인화된 콘텐츠 생성 중 오류 발생", e);
            return generateFallbackContent(request);
        }
    }
    
    /**
     * 카테고리별 콘텐츠 생성
     */
    public String generateContentByCategories(NewsletterCreateRequest request, Set<NewsCategory> categories) {
        try {
            StringBuilder content = new StringBuilder();
            content.append("<h1>카테고리별 뉴스레터 #").append(request.getNewsletterId()).append("</h1>\n");
            content.append("<p>선택하신 카테고리의 최신 뉴스를 전해드립니다.</p>\n");
            
            for (NewsCategory category : categories) {
                try {
                    ApiResponse<List<NewsResponse>> categoryNewsResponse = newsServiceClient.getNewsByCategory(
                        category.getCategoryName(), 0, 3);
                    List<NewsResponse> categoryNews = categoryNewsResponse.getData();
                    
                    content.append("<h2>").append(category.getCategoryName()).append(" ").append(category.getIcon()).append("</h2>\n");
                    
                    if (categoryNews != null && !categoryNews.isEmpty()) {
                        for (NewsResponse news : categoryNews) {
                            content.append("<div style='margin-bottom: 15px; padding: 12px; border: 1px solid #ddd; border-radius: 6px;'>\n");
                            content.append("<h3>").append(news.getTitle()).append("</h3>\n");
                            if (news.getSummary() != null && !news.getSummary().isEmpty()) {
                                content.append("<p>").append(news.getSummary()).append("</p>\n");
                            }
                            content.append("<p><small>작성일: ").append(news.getCreatedAt()).append("</small></p>\n");
                            content.append("</div>\n");
                        }
                    } else {
                        content.append("<p>").append(category.getCategoryName()).append(" 관련 최신 뉴스를 확인해보세요.</p>\n");
                    }
                } catch (Exception e) {
                    log.warn("카테고리 {} 뉴스 조회 실패", category.getCategoryName(), e);
                    content.append("<p>").append(category.getCategoryName()).append(" 관련 최신 뉴스를 확인해보세요.</p>\n");
                }
            }
            
            content.append("<p>더 많은 뉴스는 웹사이트에서 확인하실 수 있습니다.</p>\n");
            content.append("<p>감사합니다.</p>");
            
            return content.toString();
        } catch (Exception e) {
            log.error("카테고리별 콘텐츠 생성 중 오류 발생", e);
            return generateFallbackContent(request);
        }
    }
    
    /**
     * 오류 발생 시 사용할 기본 콘텐츠
     */
    private String generateFallbackContent(NewsletterCreateRequest request) {
        StringBuilder content = new StringBuilder();
        content.append("<h1>뉴스레터 #").append(request.getNewsletterId()).append("</h1>\n");
        content.append("<p>안녕하세요! 오늘의 뉴스레터입니다.</p>\n");
        content.append("<p>현재 뉴스 데이터를 불러오는 중에 일시적인 문제가 발생했습니다.</p>\n");
        content.append("<p>잠시 후 다시 시도해주시거나, 웹사이트에서 직접 확인해주세요.</p>\n");
        content.append("<p>감사합니다.</p>");
        return content.toString();
    }
    
    private String getCategoryKoreanName(NewsCategory category) {
        return category.getCategoryName();
    }
}
