package com.newsservice.news.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "category")
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "display_name")
    private String displayName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Boolean active = true;

    // NewsCrawl과의 관계
    @OneToMany(mappedBy = "category")
    private List<NewsCrawl> newsCrawls;

    // News와의 관계 (필요시)
    @OneToMany(mappedBy = "category")
    private List<News> news;
} 