package com.example.genz.Entity.Learning;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.util.List;
import java.util.UUID;
import java.util.UUID;

@Entity
@Table(name = "lesson_questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LessonQuestion {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "question_id", columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lesson_id", nullable = false)
    private Lesson lesson;

    @NotBlank
    @Column(columnDefinition = "TEXT", nullable = false)
    private String question;

    @NotBlank
    @Size(max = 500)
    @Column(name = "option_a", nullable = false)
    private String optionA;

    @NotBlank
    @Size(max = 500)
    @Column(name = "option_b", nullable = false)
    private String optionB;

    @NotBlank
    @Size(max = 500)
    @Column(name = "option_c", nullable = false)
    private String optionC;

    @NotBlank
    @Size(max = 500)
    @Column(name = "option_d", nullable = false)
    private String optionD;

    @NotBlank
    @Size(max = 1)
    @Column(name = "correct_answer", nullable = false)
    private String correctAnswer;

    @Column(name = "order_index")
    private Integer orderIndex;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserAnswer> answers;
}
