package com.example.genz.learning.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizSubmitRequest {

    @NotNull(message = "Answers list is required")
    @Size(min = 5, max = 5, message = "Quiz submission must contain exactly 5 answers")
    @Valid
    private List<QuizAnswerItem> answers;
}
