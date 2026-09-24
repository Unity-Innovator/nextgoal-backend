package com.example.genz.learning.dto;

import com.example.genz.learning.enums.AnswerChoice;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizAnswerItem {

    @NotNull(message = "Question ID is required")
    private UUID questionId;

    @NotNull(message = "Selected answer is required")
    private AnswerChoice selectedAnswer;
}
