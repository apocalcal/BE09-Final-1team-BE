package com.newsservice.news.service;

/* flaskapi 연동 클라이언트 (부모) */
/* 포트 인터페이스 */
public interface SummarizerClient {
    String summarize(String text, String type, String promptId, int lines, boolean force);
}
