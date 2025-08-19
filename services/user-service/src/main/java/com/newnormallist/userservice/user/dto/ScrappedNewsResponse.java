package com.newnormallist.userservice.user.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
public class ScrappedNewsResponse {
    private Long newsId;
    private String title;
    private String press;
    private String reporterName;
    private LocalDateTime createdAt;
    private String imageUrl;
}
