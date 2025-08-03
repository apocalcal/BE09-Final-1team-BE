package com.newsservice.news.entity;

public enum NewsCategory {
    POLITICS("정치"),
    ECONOMY("경제"),
    SOCIETY("사회"),
    IT_SCIENCE("IT/과학"),
    SPORTS("스포츠"),
    CULTURE("문화");

    private final String displayName;

    NewsCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    // displayName을 enum으로 매핑하는 정적 메서드
    public static NewsCategory fromDisplayName(String displayName) {
        for (NewsCategory category : NewsCategory.values()) {
            if (category.getDisplayName().equalsIgnoreCase(displayName)) {
                return category;
            }
        }
        throw new IllegalArgumentException("잘못된 카테고리: " + displayName);
    }
}