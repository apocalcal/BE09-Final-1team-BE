package com.newnormallist.newsservice.summarizer.client;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.newnormallist.newsservice.news.dto.SummaryRequest;
import com.newnormallist.newsservice.news.dto.SummaryResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/**
 * SummarizerClient (RestClient 버전)
 * - Flask /summary 호출 전용 경량 어댑터
 * - 모델키/프롬프트/캐시 로직은 전부 Flask 또는 상위 서비스에 위임
 */
@Component
@Slf4j
public class SummarizerClient {

    @PostConstruct
    void logBaseUrl() { log.info("[SummarizerClient] baseUrl={}", baseUrl); }

    @Value("${summarizer.base-url:http://localhost:5000}")
    private String baseUrl;

    /** 선택: 내부 인증용 API 키가 있으면 X-API-KEY로 전송 (없으면 미전송) */
    @Value("${summarizer.api-key:}")
    private String apiKey;

    /** 기본 요약 줄 수 (요청에 없을 때 사용) */
    @Value("${summarizer.default-lines:3}")
    private int defaultLines;

    /** 단건 요약 호출 */
    public SummaryResponse summarize(SummaryRequest req) {
        requireIdXorText(req);

        // 요청 바디 구성 (Flask가 타입/프롬프트 해석)
        var payload = new SingleRequest(
                req.getNewsId(),
                emptyToNull(req.getText()),
                req.getType(),
                req.getLines() != null ? req.getLines() : defaultLines,
                req.getPromptOverride(),     // 호환: prompt
                req.getPromptOverride()      // 호환: promptOverride
        );

        try {
            SingleResponse res = client().post()
                    .uri("/summary")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(SingleResponse.class);

            if (res == null) throw new SummarizerException("Empty response from summarizer");

            // SummaryResponse로 매핑
            long   newsId       = (res.newsId != null) ? res.newsId : -1L;
            String resolvedType = (res.resolvedType != null) ? res.resolvedType : "DEFAULT";
            int    lines        = (res.lines != null) ? res.lines : defaultLines;
            String summary      = (res.summary != null) ? res.summary : "";
            boolean fromCache   = Boolean.TRUE.equals(res.fromCache);

            // 작성 일시(한국 시간, ISO 8601)
            String createdAt = OffsetDateTime.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);

            return SummaryResponse.builder()
                    .newsId(newsId)
                    .resolvedType(resolvedType)
                    .lines(lines)
                    .summary(summary)
                    .cached(fromCache)
                    .createAt(createdAt)
                    .build();

        } catch (RestClientResponseException e) {
            log.error("[summarize] HTTP {} body={}", e.getRawStatusCode(), e.getResponseBodyAsString(), e);
            throw new SummarizerException("Summarizer API error: " + e.getMessage(), e);
        } catch (Exception e) {
            log.error("[summarize] unexpected error", e);
            throw new SummarizerException("Summarizer API call failed", e);
        }
    }

    /** 편의: 뉴스ID 기반 */
    public SummaryResponse summarizeByNewsId(long newsId, String type, Integer lines, String promptOverride) {
        return summarize(SummaryRequest.builder()
                .newsId(newsId).type(type).lines(lines).promptOverride(promptOverride).build());
    }

    /** 편의: 본문 기반 */
    public SummaryResponse summarizeByText(String text, String type, Integer lines, String promptOverride) {
        return summarize(SummaryRequest.builder()
                .text(text).type(type).lines(lines).promptOverride(promptOverride).build());
    }

    /* ─────────── 내부 보조 ─────────── */

    private RestClient client() {
        RestClient.Builder b = RestClient.builder().baseUrl(baseUrl);
        if (apiKey != null && !apiKey.isBlank()) {
            b.defaultHeader("X-API-KEY", apiKey);
        }
        return b.build();
    }

    private void requireIdXorText(SummaryRequest req) {
        boolean hasId = req.getNewsId() != null;
        boolean hasText = req.getText() != null && !req.getText().isBlank();
        if (hasId == hasText) {
            throw new IllegalArgumentException("newsId 또는 text 중 하나만 제공해야 합니다. (Either newsId or text must be provided exclusively)");
        }
    }

    private static String emptyToNull(String s) { return (s == null || s.isBlank()) ? null : s; }

    /* ─────────── 전송/수신 DTO ─────────── */

    /** Flask로 보내는 페이로드 (필드 최소화) */
    private record SingleRequest(
            @JsonProperty("newsId") Long   newsId,
            @JsonProperty("text")   String text,
            @JsonProperty("type")   String type,
            @JsonProperty("lines")  Integer lines,
            @JsonProperty("prompt") String prompt,                 // 호환
            @JsonProperty("promptOverride") String promptOverride  // 호환
    ) {}

    /** Flask에서 받는 응답 (snake/camel 혼용 지원) */
    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class SingleResponse {
        @JsonProperty("newsId") Long newsId;
        @JsonAlias({"resolved_type","resolvedType"}) String resolvedType;
        @JsonProperty("lines") Integer lines;
        @JsonProperty("summary") String summary;
        @JsonProperty("fromCache") Boolean fromCache;
    }

    /* ─────────── 예외 ─────────── */
    public static class SummarizerException extends RuntimeException {
        public SummarizerException(String msg) { super(msg); }
        public SummarizerException(String msg, Throwable cause) { super(msg, cause); }
    }

}
