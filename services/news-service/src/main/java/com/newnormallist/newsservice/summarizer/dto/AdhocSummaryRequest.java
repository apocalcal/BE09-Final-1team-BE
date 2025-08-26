package com.newnormallist.newsservice.summarizer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class AdhocSummaryRequest {
    @NotBlank
    private String text;

    private String type;       // 예: AIBOT

    @Min(1) @Max(10)

    private Integer lines;     // null → 3

    private String promptOverride;

    private Boolean force;

    public String getPrompt() {           // ← alias
        return this.promptOverride;
    }
}
