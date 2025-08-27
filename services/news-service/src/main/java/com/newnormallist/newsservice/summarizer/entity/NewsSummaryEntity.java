package com.newnormallist.newsservice.summarizer.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "news_summary",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_news_summary_nid_type", columnNames = {"news_id", "summary_type"})
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NewsSummaryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "news_id", nullable = false)
    private Long newsId;

    @Column(name = "summary_type", length = 50, nullable = false)
    private String summaryType;

    @Column(name = "`lines`", nullable = false) // backtick 없이 나가면서 문법 에러, MySQL 예약어 회피
    private Integer lines = 3;

    @Lob
    @Column(name = "summary_text", nullable = false, columnDefinition = "MEDIUMTEXT")
    private String summaryText;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}