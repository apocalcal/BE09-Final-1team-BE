package com.newsservice.news.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "news")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "news_id")
    private Long newsId;

    @Column(name = "old_aid", unique = true, columnDefinition = "TEXT")
    private String oldAid;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_name", nullable = false)
    private Category categoryName;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "press", nullable = false, columnDefinition = "TEXT")
    private String press;

    @Column(name = "published_at", length = 100)
    private String publishedAt;

    @Column(name = "reporter", nullable = false, columnDefinition = "TEXT")
    private String reporter;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "dedup_state", nullable = false)
    private DedupState dedupState;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "trusted", nullable = false)
    @Builder.Default
    private Boolean trusted = true;

    @Column(name = "oid_aid")
    private String oidAid;

    @Column(name = "original_news_id")
    private Long originalNewsId;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    // 뉴스레터와의 N:N 연결
    @OneToMany(mappedBy = "news")
    private List<NewsletterNews> newsletterNewsList;

    public enum Category {
        POLITICS("정치"),
        ECONOMY("경제"),
        SOCIETY("사회"),
        CULTURE("문화"),
        INTERNATIONAL("세계"),
        IT_SCIENCE("IT/과학");

        private final String description;

        Category(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum DedupState {
        REPRESENTATIVE("대표"),
        RELATED("관련"),
        KEPT("보관");

        private final String description;

        DedupState(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}