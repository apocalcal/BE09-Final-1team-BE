package com.newnormallist.newsservice.summarizer.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Flask /summary 호출 클라이언트
 * - summarize(String text, String type, int lines, String prompt) 오버로드 제공
 * - summarize(SingleRequest payload) : payload 기반 호출
 */
@Component
public class SummarizerClient {

    @Value("${summarizer.base-url:http://flaskapi:5000/summary}")
    private String baseUrl;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 외부에서도 사용할 수 있도록 public static으로 노출
     * (원하면 별도 파일로 분리해도 됩니다)
     */
    public static class SingleRequest {
        private final Long   newsId;   // 없으면 null
        private final String text;
        private final String type;
        private final int    lines;
        private final String prompt;   // prompt 하나로 통일 (override 개념 포함)

        public SingleRequest(Long newsId, String text, String type, int lines, String prompt) {
            this.newsId = newsId;
            this.text   = text;
            this.type   = type;
            this.lines  = lines;
            this.prompt = prompt;
        }

        public Long getNewsId() { return newsId; }
        public String getText() { return text; }
        public String getType() { return type; }
        public int getLines() { return lines; }
        public String getPrompt() { return prompt; }
    }

    /** 편의 오버로드: 서비스에서 이 버전만 써도 충분합니다. */
    public String summarize(String text, String type, int lines, String prompt) {
        return summarize(new SingleRequest(
                null,                       // newsId 없음
                emptyToNull(text),
                type,
                normalizeLines(lines),
                emptyToNull(prompt)
        ));
    }

    /** 실제 HTTP 호출 구현 (payload → JSON POST) */
    public String summarize(SingleRequest payload) {
        Map<String, Object> body = new HashMap<>();
        if (payload.getText() != null)   body.put("text",  payload.getText());
        if (payload.getType() != null)   body.put("type",  payload.getType());
        if (payload.getLines() > 0)      body.put("lines", payload.getLines());
        if (payload.getPrompt() != null && !payload.getPrompt().isBlank()) {
            body.put("prompt", payload.getPrompt());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> res = restTemplate.postForEntity(
                baseUrl, new HttpEntity<>(body, headers), String.class
        );

        if (!res.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Summarizer 호출 실패: " + res.getStatusCode());
        }

        String raw = res.getBody() == null ? "" : res.getBody();
        try {
            JsonNode json = objectMapper.readTree(raw);
            // Flask가 { "summary": "..."}를 반환한다고 가정
            JsonNode s = json.get("summary");
            return s != null && !s.isNull() ? s.asText("") : raw;
        } catch (Exception ignore) {
            // JSON이 아니면 원문 그대로 반환
            return raw;
        }
    }

    /* --------- 유틸 --------- */

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private static int normalizeLines(int l) {
        if (l <= 0) return 3;
        if (l > 10) return 10;
        return l;
    }
}
