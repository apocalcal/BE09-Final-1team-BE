package com.newsservice.news.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
// import org.hibernate.annotations.CreationTimestamp; // 더 이상 사용하지 않으므로 주석 처리 또는 삭제

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

    // ★★★★★ [수정] 자동 생성 기능과의 충돌을 피하기 위해 @CreationTimestamp를 제거합니다. ★★★★★
    // 이제 이 필드는 NewsServiceImpl에서 수동으로 설정한 값에만 의존하게 됩니다.
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
