package com.newnormallist.newsservice.exception;

public class NewsNotFoundException extends RuntimeException {
    public NewsNotFoundException(String message) {
        super(message);
    }
    
    public NewsNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
