package com.newnormallist.newsservice.recommendation.model;

import com.newnormallist.newsservice.recommendation.entity.Category;
import lombok.*;

// 내부 계산/전달용 (Category, score) 페어
@Getter @AllArgsConstructor
public class CategoryScore {
    private final Category category;
    private final double score;
}