package com.newnormallist.newsservice.summarizer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;

/**
 *요약 생성/조회 요청 DTO
 * - lines: null 이면 기본 3
 * - force: null/false => 기본 falise 캐시 무시 재생성 시 true
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SummaryRequest {

    private Long newsId;    // newsId

    private String text;

    private String type;    // 카테고리/요약 타입

    @Min(1) @Max(10)
    private Integer lines;  // SummarizerClient에서 기본 3 사용

    private String promptOverride;

    private Boolean force;

    public String getPrompt() {           // ← alias
        return this.promptOverride;
    }

    /** Options → Request : 최소 매핑 (공백 문자열은 null 처리) */
    public static SummaryRequest from(SummaryOptions o) {
        if (o == null) throw new IllegalArgumentException("요청 바디가 비어 있습니다.");
        return SummaryRequest.builder()
                .newsId(o.getNewsId())
                .text(blankToNull(o.getText()))
                .type(o.getType())
                .lines(o.getLines())
                .promptOverride(blankToNull(o.getPromptOverride()))
                .build();
    }

    private static String blankToNull(String s){
        return (s == null || s.isBlank()) ? null : s;
    }
}
