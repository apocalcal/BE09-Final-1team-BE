package com.newnormallist.newsservice.recommendation.util;

import com.newnormallist.newsservice.recommendation.entity.Category;
import com.newnormallist.newsservice.recommendation.repository.UserCategoryRepository;

import java.util.*;

public class PrefVectorHelper {

    private final UserCategoryRepository userCategoryRepository;

    public PrefVectorHelper(UserCategoryRepository repo) {
        this.userCategoryRepository    = repo;
    }

    /**
     * 선호 카테고리 분포 P(c) 생성: 선택된 카테고리만 1/k, 나머지는 0
     */
    public Map<Category, Double> buildP(Long userId) {
        List<Category> cats = userCategoryRepository.findCategoriesByUserId(userId);
        Map<Category, Double> P = new EnumMap<>(Category.class);
        int k = cats.size();
        if (k == 0) return P; // 빈 맵(모두 0) 반환
        double w = 1.0 / k;
        for (Category c : cats) P.put(c, w);
        return P;
    }
}
