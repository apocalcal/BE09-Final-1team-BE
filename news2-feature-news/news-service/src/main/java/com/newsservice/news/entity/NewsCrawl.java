package com.newsservice.news.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "news")
public class NewsCrawl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "row_id")
    private Long id;

    @Column(name = "link", columnDefinition = "TEXT")
    private String linkId;

    @Column(name = "title", columnDefinition = "TEXT")
    private String title;

    @Column(name = "press", columnDefinition = "TEXT")
    private String press;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;


    @Column(name = "reporter_name")
    private String reporterName;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;
}