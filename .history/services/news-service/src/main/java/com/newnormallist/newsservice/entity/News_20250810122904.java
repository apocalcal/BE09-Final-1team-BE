package com.newnormallist.newsservice.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "category_name", nullable = false, length = 13)
    private CategoryType categoryName;

    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(name = "press", nullable = false, columnDefinition = "TEXT")
    private String press;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "reporter", nullable = false, columnDefinition = "TEXT")
    private String reporter;

    @Enumerated(EnumType.STRING)
    @Column(name = "dedup_state", nullable = false, length = 14)
    private DedupState dedupState;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "trusted")
    private Integer trusted;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "oid_aid")
    private String oidAid;

    @Column(name = "original_news_id")
    private Long originalNewsId;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    public enum CategoryType {
        POLITICS("정치"),
        ECONOMY("경제"),
        SOCIETY("사회"),
        CULTURE("문화"),
        INTERNATIONAL("세계"),
        IT_SCIENCE("IT_SCIENCE");

        private final String description;

        CategoryType(String description) {
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
