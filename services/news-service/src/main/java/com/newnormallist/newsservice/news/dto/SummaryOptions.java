package com.newnormallist.newsservice.news.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SummaryOptions {
    /* newsId 또는 text 중 하나 */
    private Long newsId;
    private String text;

    private String type;    // PromptManager에 전달할 타입
    private Integer lines;  // 기본값 3은 Client에서 보정

    private String promptOverride;  // 실제 필드명에 맞춰 위 from에서 매핑
}
