package com.newnormallist.userservice.clients;

import com.newnormallist.userservice.config.CustomPageImpl;
import com.newnormallist.userservice.user.dto.ScrappedNewsResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "news-service")
public interface NewsServiceClient {

    @GetMapping("/api/news/scraps")
    CustomPageImpl<ScrappedNewsResponse> getScrappedNews(
        @RequestHeader("X-User-Id") Long userId,
        @RequestParam("page") int page,
        @RequestParam("size") int size,
        @RequestParam("sort") String sort
    );
}
