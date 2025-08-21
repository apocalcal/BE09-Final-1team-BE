package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.SummaryOptions;
import com.newnormallist.newsservice.news.dto.SummaryRequest;
import com.newnormallist.newsservice.news.dto.SummaryResponse;
import com.newnormallist.newsservice.news.service.NewsSummaryService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/news")
@RequiredArgsConstructor
@Slf4j
@Validated
@Tag(name = "뉴스 요약", description = "newsId 또는 text 중 하나는 필수. type/lines 선택.")
public class NewsSummaryController {

    private final NewsSummaryService newsSummaryService;

    @PostMapping(value = "/summary",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SummaryResponse summarize(@Valid @org.springframework.web.bind.annotation.RequestBody SummaryOptions body) {
        // XOR 검증
        boolean hasId = body.getNewsId() != null;
        boolean hasText = body.getText() != null && !body.getText().isBlank();
        if (hasId == hasText) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "newsId와 text는 동시에 보낼 수 없으며, 둘 중 정확히 하나만 제공해야 합니다.");
        }

        // Options → Request 변환 (컨트롤러에서 API 계약만 담당)
        SummaryRequest req = SummaryRequest.from(body);
        return newsSummaryService.summarize(req);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArg(IllegalArgumentException e){
        return Map.of("error", "bad_request", "message", e.getMessage());
    }
}
