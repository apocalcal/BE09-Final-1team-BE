package com.newsletterservice.service;

import com.newsletterservice.client.NewsServiceClient;
import com.newsletterservice.client.UserServiceClient;
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
    
    // Default categories for fallback content
    private static final List<NewsCategory> DEFAULT_CATEGORIES = Arrays.asList(
        NewsCategory.POLITICS, 
        NewsCategory.ECONOMY, 
        NewsCategory.SOCIETY
    );
    
    // Personalized content categories
    private static final List<NewsCategory> PERSONALIZED_CATEGORIES = Arrays.asList(
        NewsCategory.POLITICS, 
        NewsCategory.ECONOMY, 
        NewsCategory.SOCIETY,
        NewsCategory.IT_SCIENCE
    );
    
    /**
     * Get latest news data for newsletter
     */
    public List<NewsResponse> getLatestNewsData(NewsletterCreateRequest request) {
        try {
            log.info("Fetching latest news data for newsletterId: {}", request.getNewsletterId());
            return fetchLatestNews(5);
        } catch (Exception e) {
            log.error("Error fetching latest news data", e);
            return List.of();
        }
    }
    
    /**
     * Get personalized news data for specific users
     */
    public List<NewsResponse> getPersonalizedNewsData(NewsletterCreateRequest request, List<Long> userIds) {
        try {
            log.info("Fetching personalized news data for newsletterId: {}, userIds: {}", 
                    request.getNewsletterId(), userIds);
            
            List<NewsResponse> trendingNews = fetchTrendingNews(24, 3);
            List<NewsResponse> categoryNews = fetchNewsByCategories(PERSONALIZED_CATEGORIES, 2);
            
            // Combine trending and category news
            List<NewsResponse> allNews = new java.util.ArrayList<>();
            allNews.addAll(trendingNews);
            allNews.addAll(categoryNews);
            
            return allNews;
        } catch (Exception e) {
            log.error("Error fetching personalized news data", e);
            return List.of();
        }
    }
    
    /**
     * Get news data by specific categories
     */
    public List<NewsResponse> getCategoryNewsData(NewsletterCreateRequest request, Set<NewsCategory> categories) {
        try {
            log.info("Fetching category news data for newsletterId: {}, categories: {}", 
                    request.getNewsletterId(), categories);
            
            return fetchNewsByCategories(categories, 3);
        } catch (Exception e) {
            log.error("Error fetching category news data", e);
            return List.of();
        }
    }
    
    /**
     * Get subscriber information
     */
    public List<UserResponse> getSubscriberInfo(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        
        try {
            ApiResponse<List<UserResponse>> usersResponse = userServiceClient.getUsersByIds(userIds);
            return usersResponse.getData() != null ? usersResponse.getData() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch subscriber information", e);
            return List.of();
        }
    }
    
    /**
     * Get default categories
     */
    public List<NewsCategory> getDefaultCategories() {
        return DEFAULT_CATEGORIES;
    }
    
    /**
     * Get personalized categories
     */
    public List<NewsCategory> getPersonalizedCategories() {
        return PERSONALIZED_CATEGORIES;
    }
    
    // ===== Private helper methods =====
    
    private List<NewsResponse> fetchLatestNews(int limit) {
        try {
            ApiResponse<List<NewsResponse>> response = newsServiceClient.getLatestNews(null, limit);
            return response.getData() != null ? response.getData() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch latest news", e);
            return List.of();
        }
    }
    
    private List<NewsResponse> fetchTrendingNews(int hours, int limit) {
        try {
            ApiResponse<List<NewsResponse>> response = newsServiceClient.getTrendingNews(hours, limit);
            return response.getData() != null ? response.getData() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch trending news", e);
            return List.of();
        }
    }
    
    private List<NewsResponse> fetchNewsByCategories(Iterable<NewsCategory> categories, int newsPerCategory) {
        List<NewsResponse> allNews = new java.util.ArrayList<>();
        
        for (NewsCategory category : categories) {
            try {
                List<NewsResponse> categoryNews = fetchNewsByCategory(category, 0, newsPerCategory);
                allNews.addAll(categoryNews);
            } catch (Exception e) {
                log.warn("Failed to fetch news for category: {}", category.getCategoryName(), e);
            }
        }
        
        return allNews;
    }
    
    private List<NewsResponse> fetchNewsByCategory(NewsCategory category, int page, int size) {
        try {
            ApiResponse<List<NewsResponse>> response = newsServiceClient.getNewsByCategory(
                category.getCategoryName(), page, size);
            return response.getData() != null ? response.getData() : List.of();
        } catch (Exception e) {
            log.warn("Failed to fetch news for category: {}", category.getCategoryName(), e);
            return List.of();
        }
    }
}
