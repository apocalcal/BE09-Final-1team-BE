package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.NewsListResponse;
import com.newnormallist.newsservice.news.exception.UnauthenticatedUserException;
import com.newnormallist.newsservice.news.service.MyPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/news/mypage")
@RequiredArgsConstructor
public class MyPageController {

    private final MyPageService myPageService;

    @GetMapping("/scraps")
    public ResponseEntity<Page<NewsListResponse>> getMyScraps(
            @AuthenticationPrincipal String userIdString,
            @RequestParam(required = false) String category,
            Pageable pageable) {
        Long userId = getUserIdOrThrow(userIdString);

        // 프론트엔드에서 요청한 페이지 번호와 정렬은 유지하되, 페이지 크기는 10으로 고정합니다.
        Pageable fixedPageable = PageRequest.of(pageable.getPageNumber(), 10, pageable.getSort());

        Page<NewsListResponse> scraps = myPageService.getScrappedNews(userId, category, fixedPageable);
        return ResponseEntity.ok(scraps);
    }

    @DeleteMapping("/scraps/{newsId}")
    public ResponseEntity<Void> deleteScrap(
            @AuthenticationPrincipal String userIdString,
            @PathVariable Long newsId) {
        Long userId = getUserIdOrThrow(userIdString);
        myPageService.deleteScrap(userId, newsId);
        return ResponseEntity.noContent().build();
    }

    /**
     * 인증 정보에서 사용자 ID를 가져오거나, 정보가 없으면 예외를 발생시킵니다.
     * @param userIdString @AuthenticationPrincipal로 주입된 사용자 ID 문자열
     * @return Long 타입의 사용자 ID
     * @throws UnauthenticatedUserException 사용자 인증 정보가 없을 경우 발생
     */
    private Long getUserIdOrThrow(String userIdString) {
        // Spring Security는 인증되지 않은 사용자를 "anonymousUser" 문자열로 전달할 수 있습니다.
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        try {
            return Long.parseLong(userIdString);
        } catch (NumberFormatException e) {
            // "anonymousUser"가 아닌 다른 비정상적인 문자열이 들어올 경우를 대비한 방어 코드
            throw new UnauthenticatedUserException("유효하지 않은 사용자 ID 형식입니다: " + userIdString);
        }
    }
}