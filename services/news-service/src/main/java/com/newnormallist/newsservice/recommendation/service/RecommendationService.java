package com.newnormallist.newsservice.recommendation.service;

// 피드 조립 서비스 인터페이스.
// 구현체(예: RecommendationServiceImpl)는:
// UserPrefVectorRepository.findTop3ByUserId(userId)로 top3 카테고리 확보
// 각 카테고리에서 최신 7/5/3 ID 수집
// findByIds로 뉴스 메타 일괄 조회
// DTO로 매핑해 반환

public interface RecommendationService {
    
}
