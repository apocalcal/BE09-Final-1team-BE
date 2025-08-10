package com.newsservice.news.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "news")
public class News {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "original_news_id", nullable = false)
    private Long originalNewsId;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private Integer trusted;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // NewsCrawl과의 관계 설정
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_news_id", insertable = false, updatable = false)
    private NewsCrawl originalNews;

    // Category enum 사용
    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false)
    private Category category;

    // 뉴스레터와의 N:N 연결
    @OneToMany(mappedBy = "news")
    private List<NewsletterNews> newsletterNewsList;

    @Column(name = "image_url", columnDefinition = "TEXT")
    private String imageUrl;

    @Column(name = "oid_aid")
    private String oidAid;
}