package com.newsservice.news.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 *요약 생성/조회 요청 DTO
 * - lines: null 이면 기본 3
 * - force: null/false => 기본 falise 캐시 무시 재생성 시 true
 */
@Getter
@Setter
public class SummaryRequest {

    @NotNull
    private Long newsId;

    private Integer lines;
    private Boolean force;
}
