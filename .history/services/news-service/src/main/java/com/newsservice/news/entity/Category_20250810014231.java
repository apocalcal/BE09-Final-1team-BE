package com.newsservice.news.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Category {
    POLITICS("정치", "🏛️"),
    ECONOMY("경제", "💰"),
    SOCIETY("사회", "👥"),
    CULTURE("생활/문화", "🎭"),
    INTERNATIONAL("세계", "🌍"),
    IT_SCIENCE("IT/과학", "💻");
    
    private final String categoryName;
    private final String icon;
}
