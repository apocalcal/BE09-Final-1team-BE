package com.newnormallist.userservice.user.entity;

public enum NewsCategory {
    POLITICS("정치"),
    ECONOMY("경제"),
    SOCIETY("사회"),
    CULTURE("문화"),
    INTERNATIONAL("국제"),
    SPORTS("스포츠"),
    IT("IT/과학"),
    ENTERTAINMENT("연예");
    
    private final String displayName;
    
    NewsCategory(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
}