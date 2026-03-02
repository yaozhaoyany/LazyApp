package com.lazyapp.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_plans")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDate planDate;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String planJson;

    private boolean skippedDay;

    @Column(nullable = false, updatable = false)
    private LocalDateTime generatedAt;

    @PrePersist
    protected void onCreate() {
        generatedAt = LocalDateTime.now();
    }
}
