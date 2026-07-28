package com.example.genz.Entity.Learning;

import com.example.genz.Enums.LessonCategory;
import com.example.genz.Enums.LessonDifficulty;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Lesson {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "lesson_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false)
    private String title;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LessonCategory category;

    @Column(columnDefinition = "TEXT")
    private String content;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LessonDifficulty difficulty;

    @Column(name = "points_reward")
    private Integer pointsReward;

    @Column(name = "is_active")
    private Boolean isActive;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LessonQuestion> questions;

    @OneToMany(mappedBy = "lesson", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserLesson> userLessons;
}
