package com.newnormallist.newsservice.news.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "news_complaint")
public class NewsComplaint {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long complaintId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long newsId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
