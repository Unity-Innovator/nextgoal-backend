package com.example.genz.learning.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizStartResponse {
    private UUID quizAttemptId;
    private Integer totalQuestions;
    private List<QuizQuestionResponse> questions;
    private Integer remainingEnergy;
}
