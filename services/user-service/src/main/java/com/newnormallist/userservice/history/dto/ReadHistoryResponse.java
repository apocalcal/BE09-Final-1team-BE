package com.newnormallist.userservice.history.dto;

import com.newnormallist.userservice.history.entity.UserReadHistory;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ReadHistoryResponse {
    private final Long newsId;
    private final LocalDateTime updatedAt;

    public ReadHistoryResponse(UserReadHistory history) {
        this.newsId = history.getNewsId();
        this.updatedAt = history.getUpdatedAt();
    }
}
