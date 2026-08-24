package com.example.genz.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizResultResponse {
    private UUID quizAttemptId;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Integer scorePercentage;
    private Integer baseXp;
    private Integer bonusXp;
    private Integer totalXpEarned;
    private Integer remainingEnergy;
    private LocalDateTime completedAt;
}
