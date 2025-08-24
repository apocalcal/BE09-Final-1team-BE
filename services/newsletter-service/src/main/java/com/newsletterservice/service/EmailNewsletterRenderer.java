package com.newsletterservice.service;

import com.newsletterservice.dto.NewsletterContent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNewsletterRenderer {
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    
    /**
     * 뉴스레터 콘텐츠를 이메일용 HTML로 렌더링
     */
    public String renderToHtml(NewsletterContent content) {
        log.info("Rendering newsletter content to HTML for user: {}", content.getUserId());
        
        StringBuilder html = new StringBuilder();
        
        // HTML 헤더
        html.append("<!DOCTYPE html>\n");
        html.append("<html lang='ko'>\n");
        html.append("<head>\n");
        html.append("    <meta charset='UTF-8'>\n");
        html.append("    <meta name='viewport' content='width=device-width, initial-scale=1.0'>\n");
        html.append("    <title>").append(content.getTitle()).append("</title>\n");
        html.append("    <style>\n");
        html.append("        body { font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; margin: 0; padding: 20px; background-color: #f5f5f5; }\n");
        html.append("        .container { max-width: 600px; margin: 0 auto; background-color: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }\n");
        html.append("        .header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 30px; text-align: center; }\n");
        html.append("        .header h1 { margin: 0; font-size: 24px; font-weight: 300; }\n");
        html.append("        .content { padding: 30px; }\n");
        html.append("        .section { margin-bottom: 30px; }\n");
        html.append("        .section-header { border-bottom: 2px solid #667eea; padding-bottom: 10px; margin-bottom: 20px; }\n");
        html.append("        .section-title { font-size: 20px; color: #333; margin: 0; }\n");
        html.append("        .section-description { color: #666; font-size: 14px; margin-top: 5px; }\n");
        html.append("        .article { border: 1px solid #e0e0e0; border-radius: 6px; padding: 15px; margin-bottom: 15px; background-color: #fafafa; }\n");
        html.append("        .article:hover { border-color: #667eea; box-shadow: 0 2px 8px rgba(102, 126, 234, 0.2); }\n");
        html.append("        .article-title { font-size: 16px; font-weight: 600; color: #333; margin: 0 0 8px 0; }\n");
        html.append("        .article-title a { color: #333; text-decoration: none; }\n");
        html.append("        .article-title a:hover { color: #667eea; }\n");
        html.append("        .article-summary { color: #666; font-size: 14px; line-height: 1.5; margin-bottom: 10px; }\n");
        html.append("        .article-meta { display: flex; justify-content: space-between; align-items: center; font-size: 12px; color: #999; }\n");
        html.append("        .article-category { background-color: #667eea; color: white; padding: 2px 8px; border-radius: 12px; font-size: 11px; }\n");
        html.append("        .personalized-badge { background-color: #ff6b6b; color: white; padding: 2px 6px; border-radius: 10px; font-size: 10px; margin-left: 5px; }\n");
        html.append("        .footer { background-color: #f8f9fa; padding: 20px; text-align: center; color: #666; font-size: 12px; }\n");
        html.append("        .personalization-info { background-color: #e3f2fd; border-left: 4px solid #2196f3; padding: 15px; margin-bottom: 20px; border-radius: 4px; }\n");
        html.append("    </style>\n");
        html.append("</head>\n");
        html.append("<body>\n");
        
        // 컨테이너 시작
        html.append("<div class='container'>\n");
        
        // 헤더
        html.append("    <div class='header'>\n");
        html.append("        <h1>📰 ").append(content.getTitle()).append("</h1>\n");
        if (content.isPersonalized()) {
            html.append("        <p>🎯 당신만을 위한 맞춤 뉴스레터</p>\n");
        }
        html.append("        <p>").append(content.getGeneratedAt().format(DATE_FORMATTER)).append(" 발행</p>\n");
        html.append("    </div>\n");
        
        // 콘텐츠 시작
        html.append("    <div class='content'>\n");
        
        // 개인화 정보 (개인화된 경우)
        if (content.isPersonalized()) {
            html.append("        <div class='personalization-info'>\n");
            html.append("            <strong>🎯 개인화 정보</strong><br>\n");
            html.append("            이 뉴스레터는 당신의 관심사와 행동 패턴을 분석하여 맞춤 구성되었습니다.\n");
            html.append("        </div>\n");
        }
        
        // 섹션들 렌더링
        for (NewsletterContent.Section section : content.getSections()) {
            html.append(renderSection(section));
        }
        
        html.append("    </div>\n");
        
        // 푸터
        html.append("    <div class='footer'>\n");
        html.append("        <p>이 뉴스레터는 자동으로 생성되었습니다.</p>\n");
        html.append("        <p>구독 해지나 설정 변경은 웹사이트에서 가능합니다.</p>\n");
        html.append("    </div>\n");
        
        html.append("</div>\n");
        html.append("</body>\n");
        html.append("</html>");
        
        return html.toString();
    }
    
    /**
     * 섹션 렌더링
     */
    private String renderSection(NewsletterContent.Section section) {
        StringBuilder html = new StringBuilder();
        
        html.append("        <div class='section'>\n");
        html.append("            <div class='section-header'>\n");
        html.append("                <h2 class='section-title'>").append(section.getHeading()).append("</h2>\n");
        if (section.getDescription() != null && !section.getDescription().isEmpty()) {
            html.append("                <p class='section-description'>").append(section.getDescription()).append("</p>\n");
        }
        html.append("            </div>\n");
        
        // 아티클들 렌더링
        for (NewsletterContent.Article article : section.getArticles()) {
            html.append(renderArticle(article, section.getSectionType()));
        }
        
        html.append("        </div>\n");
        
        return html.toString();
    }
    
    /**
     * 아티클 렌더링
     */
    private String renderArticle(NewsletterContent.Article article, String sectionType) {
        StringBuilder html = new StringBuilder();
        
        html.append("            <div class='article'>\n");
        
        // 제목
        html.append("                <h3 class='article-title'>\n");
        html.append("                    <a href='").append(article.getUrl()).append("' target='_blank'>\n");
        html.append("                        ").append(article.getTitle()).append("\n");
        if ("PERSONALIZED".equals(sectionType) && article.getPersonalizedScore() != null && article.getPersonalizedScore() > 0.7) {
            html.append("                        <span class='personalized-badge'>추천</span>\n");
        }
        html.append("                    </a>\n");
        html.append("                </h3>\n");
        
        // 요약
        if (article.getSummary() != null && !article.getSummary().isEmpty()) {
            html.append("                <p class='article-summary'>").append(article.getSummary()).append("</p>\n");
        }
        
        // 메타 정보
        html.append("                <div class='article-meta'>\n");
        html.append("                    <span class='article-category'>").append(article.getCategory()).append("</span>\n");
        if (article.getPublishedAt() != null) {
            html.append("                    <span>").append(article.getPublishedAt().format(DATE_FORMATTER)).append("</span>\n");
        }
        html.append("                </div>\n");
        
        html.append("            </div>\n");
        
        return html.toString();
    }
}
