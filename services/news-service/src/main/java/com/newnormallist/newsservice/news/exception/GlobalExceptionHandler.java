package com.newnormallist.newsservice.news.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(value = { NewsHiddenException.class })
    protected ResponseEntity<Object> handleNewsHidden(RuntimeException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        return handleExceptionInternal(ex, bodyOfResponse,
          new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler(value = { NewsNotFoundException.class })
    protected ResponseEntity<Object> handleNewsNotFound(RuntimeException ex, WebRequest request) {
        String bodyOfResponse = ex.getMessage();
        return handleExceptionInternal(ex, bodyOfResponse,
          new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(value = { Exception.class })
    protected ResponseEntity<Object> handleAllOtherExceptions(Exception ex, WebRequest request) {
        String bodyOfResponse = "서버 내부에서 예상치 못한 오류가 발생했습니다.";
        // 에러의 원인을 로그로 남기는 것이 매우 중요합니다.
        ex.printStackTrace();
        // 500 Internal Server Error 상태 코드로 응답합니다.
        return handleExceptionInternal(ex, bodyOfResponse,
          new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR, request);
    }
}
