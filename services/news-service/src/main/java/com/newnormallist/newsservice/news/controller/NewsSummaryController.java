package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.SummaryOptions;
import com.newnormallist.newsservice.news.dto.SummaryRequest;
import com.newnormallist.newsservice.news.dto.SummaryResponse;
import com.newnormallist.newsservice.news.service.NewsSummaryService;
import io.swagger.v3.oas.annotations.Operation;
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
@Tag(name = "뉴스 요약", description = "뉴스를 요약하는 기능")
public class NewsSummaryController {

    private final NewsSummaryService newsSummaryService;

    @Operation(
            summary = "뉴스 요약 생성",
            description = "newsId 또는 text 중 하나를 입력해야 합니다. type/lines는 선택값입니다."
    )
    @PostMapping(value = "/summary",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public SummaryResponse summarize(@Valid @org.springframework.web.bind.annotation.RequestBody SummaryOptions body) {

        // XOR 검증
        boolean hasId = body.getNewsId() != null;
        boolean hasText = body.getText() != null && !body.getText().isBlank();

        log.info("[NSC] 입력 수신: newsId={}, textPresent={}, type={}, lines={}",
                body.getNewsId(), hasText, body.getType(), body.getLines());

        boolean xorOk = hasId ^ hasText; // 정확한 XOR
        if (!xorOk) {
            log.warn("[NSC] XOR 실패: hasId={}, hasText={}", hasId, hasText);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "newsId와 text는 동시에 보낼 수 없으며, 둘 중 정확히 하나만 제공 해야 합니다.");
        } else {
            log.info("[NSC] XOR 통과 ({}만 제공됨)", hasId ? "newsId" : "text");
        }

        log.info("[NewsSummaryController] XOR PASSED: using {}",
                hasId ? ("newsId=" + body.getNewsId())
                        : ("textLen=" + (body.getText() == null ? 0 : body.getText().length())));

        // Options → Request 변환 (컨트롤러에서 API 계약만 담당)
        SummaryRequest req = SummaryRequest.from(body);
        return newsSummaryService.summarize(req);
    }

    // 예외 매핑
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, Object> handleIllegalArg(IllegalArgumentException e){
        return Map.of("error", "bad_request", "message", e.getMessage());
    }

    // ── 로그용 보조 메서드 (민감 데이터 노출 방지)
    private static String summarizeForLog(SummaryOptions b) {
        Integer textLen = (b.getText() == null ? null : b.getText().length());
        return String.format("{newsId=%s, textLen=%s, type=%s, lines=%s}",
                b.getNewsId(), textLen, b.getType(), b.getLines());
    }}
