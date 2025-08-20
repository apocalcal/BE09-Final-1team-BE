package com.newsservice.news.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;


// 어댑터(구현) - Rest 호출
@Component
@RequiredArgsConstructor
@Slf4j
public class SummarizerClientRest implements SummarizerClient {
    private final RestClient summaryRestClient; // base-url: http://flaskapi:5000

    record FlaskSummaryResponse(String summary, Boolean cached) {}

    @Override
    public String summarize(String text, String type, String promptId, int lines, boolean force) {
        var body = Map.of(
                "text", text, "summary_type", type, "prompt", promptId,
                "lines", lines, "force", force
        );
        var response = summaryRestClient.post().uri("/summary").body(body)
                .retrieve().body(FlaskSummaryResponse.class);
        if (response == null || response.summary() == null) throw new IllegalStateException("flask summary empty");
        return response.summary();
    }
}
