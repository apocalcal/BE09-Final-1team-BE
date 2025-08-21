package com.newnormallist.userservice.user.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String name;

    @Column(name = "birth_year", nullable = false)
    private Integer birthYear;

    @Column(name = "gender", nullable = false)
    private Gender gender;

    @Column(name = "letter_ok", nullable = false)
    @Builder.Default
    private Boolean letterOk = false;

    @ElementCollection(fetch = FetchType.LAZY)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "user_categories", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "category")
    private Set<NewsCategory> hobbies = new HashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private UserRole role = UserRole.USER;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "user_status", nullable = false)
    @Builder.Default
    private UserStatus status = UserStatus.ACTIVE;

    public void updatePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void updateProfile(@NotNull(message = "뉴스레터 수신 동의 여부는 필수입니다.") Boolean letterOk, Set<String> hobbies) {
        this.letterOk = letterOk;

        if (hobbies != null) {
            this.hobbies = hobbies.stream()
                    .map(hobby -> {
                        try {
                            return NewsCategory.valueOf(hobby.toUpperCase());
                        } catch (IllegalArgumentException e) {
                            throw new IllegalArgumentException("유효하지 않은 관심사 카테고리입니다: " + hobby);
                        }
                    })
                    .collect(java.util.stream.Collectors.toSet());
        }
    }

    public void changeStatus(UserStatus userStatus) {
        this.status = userStatus;
    }
}