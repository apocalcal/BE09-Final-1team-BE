package com.newnormallist.newsservice.news.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SummaryResponse {
    private Long newsId;
    private String resolvedType;
    private Integer lines;      // 정보용(정책값). DB에는 저장 안 함
    private String summary;
    private boolean cached;     // 캐시 히트 여부(Flask 판단)
    private String createAt;

}
