package com.example.genz.learning.entity;

import com.example.genz.auth.entity.User;
import com.example.genz.learning.enums.LessonStatus;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_lessons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserLesson {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "user_lesson_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LessonStatus status;

    private Integer score;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
