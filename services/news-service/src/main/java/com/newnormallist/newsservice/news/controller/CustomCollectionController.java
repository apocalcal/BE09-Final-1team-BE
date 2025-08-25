package com.newnormallist.newsservice.news.controller;

import com.newnormallist.newsservice.news.dto.collection.CollectionCreateRequest;
import com.newnormallist.newsservice.news.dto.collection.CollectionResponse;
import com.newnormallist.newsservice.news.dto.collection.NewsToCollectionRequest;
import com.newnormallist.newsservice.news.dto.NewsListResponse;
import com.newnormallist.newsservice.news.exception.UnauthenticatedUserException;
import com.newnormallist.newsservice.news.service.CustomCollectionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/collections")
@RequiredArgsConstructor
public class CustomCollectionController {

    private final CustomCollectionService customCollectionService;

    // POST /api/collections: 새로운 컬렉션 생성
    @PostMapping
    public ResponseEntity<CollectionResponse> createCollection(
            @RequestBody CollectionCreateRequest request,
            @AuthenticationPrincipal String userIdString) {
        Long userId = getUserIdOrThrow(userIdString);
        CollectionResponse response = customCollectionService.createCollection(userId, request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/collections: 사용자의 모든 컬렉션 목록 조회
    @GetMapping
    public ResponseEntity<List<CollectionResponse>> getCollections(
            @AuthenticationPrincipal String userIdString) {
        Long userId = getUserIdOrThrow(userIdString);
        List<CollectionResponse> collections = customCollectionService.getUserCollections(userId);
        return ResponseEntity.ok(collections);
    }

    // POST /api/collections/:id/news: 특정 컬렉션에 뉴스 추가
    @PostMapping("/{collectionId}/news")
    public ResponseEntity<Void> addNewsToCollection(
            @PathVariable("collectionId") Long collectionId,
            @RequestBody NewsToCollectionRequest request,
            @AuthenticationPrincipal String userIdString) {
        Long userId = getUserIdOrThrow(userIdString);
        customCollectionService.addNewsToCollection(userId, collectionId, request.getNewsId());
        return ResponseEntity.ok().build();
    }

    // DELETE /api/collections/:id/news/:newsId: 특정 컬렉션에서 뉴스 삭제
    @DeleteMapping("/{collectionId}/news/{newsId}")
    public ResponseEntity<Void> deleteNewsFromCollection(
            @PathVariable("collectionId") Long collectionId,
            @PathVariable("newsId") Long newsId,
            @AuthenticationPrincipal String userIdString) {
        Long userId = getUserIdOrThrow(userIdString);
        customCollectionService.deleteNewsFromCollection(userId, collectionId, newsId);
        return ResponseEntity.noContent().build();
    }

    // GET /api/collections/:id: 특정 컬렉션에 속한 모든 뉴스 조회
    @GetMapping("/{collectionId}")
    public ResponseEntity<Page<NewsListResponse>> getNewsInCollection(
            @PathVariable("collectionId") Long collectionId,
            @AuthenticationPrincipal String userIdString,
            Pageable pageable) {
        Long userId = getUserIdOrThrow(userIdString);
        Page<NewsListResponse> news = customCollectionService.getNewsInCollection(userId, collectionId, pageable);
        return ResponseEntity.ok(news);
    }

    // DELETE /api/collections/:id: 컬렉션 삭제
    @DeleteMapping("/{collectionId}")
    public ResponseEntity<Void> deleteCollection(
            @PathVariable("collectionId") Long collectionId,
            @AuthenticationPrincipal String userIdString) {
        Long userId = getUserIdOrThrow(userIdString);
        customCollectionService.deleteCollection(userId, collectionId);
        return ResponseEntity.noContent().build();
    }

    private Long getUserIdOrThrow(String userIdString) {
        if (userIdString == null || "anonymousUser".equals(userIdString)) {
            throw new UnauthenticatedUserException("사용자 인증 정보가 없습니다. 로그인이 필요합니다.");
        }
        try {
            return Long.parseLong(userIdString);
        } catch (NumberFormatException e) {
            throw new UnauthenticatedUserException("유효하지 않은 사용자 ID 형식입니다: " + userIdString);
        }
    }
}
