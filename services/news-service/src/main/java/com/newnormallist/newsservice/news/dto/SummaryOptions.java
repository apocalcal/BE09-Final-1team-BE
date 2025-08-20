package com.newsservice.news.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SummaryOptions {

    private Integer lines;  // null이면 기본 3
    private Boolean force;  // null이면 false

    @JsonIgnore
    public int safeLines() {
        int v = (lines == null) ? 3 : lines;
        return Math.max(1, Math.min(10, v));
    }

    @JsonIgnore
    public boolean isForce() {
        return Boolean.TRUE.equals(force);
    }

}
