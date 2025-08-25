package com.newnormallist.newsservice.news.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 리소스를 찾을 수 없을 때 발생하는 예외
 * 이 예외가 컨트롤러 계층까지 전달되면, Spring은 자동으로 404 Not Found HTTP 상태 코드를 응답
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resourceName, String fieldName, Object fieldValue) {
        super(String.format("%s not found with %s : '%s'", resourceName, fieldName, fieldValue));
    }
}
