package com.newsletterservice.service;

import com.newsletterservice.dto.SubscriptionRequest;
import com.newsletterservice.dto.SubscriptionResponse;

import java.util.List;

public interface SubscriptionService {
    
    SubscriptionResponse subscribe(SubscriptionRequest request, String userId);
    
    SubscriptionResponse getSubscription(Long id);
    
    List<SubscriptionResponse> getSubscriptionsByUser(String userId);
    
    void unsubscribe(Long id);
    
    List<SubscriptionResponse> getActiveSubscriptions();
}
